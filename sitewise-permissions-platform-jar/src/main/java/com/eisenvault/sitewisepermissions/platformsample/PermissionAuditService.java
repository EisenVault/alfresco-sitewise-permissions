/*
 * Copyright 2025 EisenVault
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eisenvault.sitewisepermissions.platformsample;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Service for tracking permission changes in Alfresco.
 * This service maintains a custom audit trail of permission grants and revokes.
 */
public class PermissionAuditService {
    
    private static Log logger = LogFactory.getLog(PermissionAuditService.class);
    
    private JdbcTemplate jdbcTemplate;
    private PermissionService permissionService;
    private PersonService personService;
    private AuthorityService authorityService;
    private NodeService nodeService;
    private NamespacePrefixResolver namespacePrefixResolver;
    
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }
    
    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }
    
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver) {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }
    
    /**
     * Record a permission grant event
     * @param nodeRef the node the permission was granted on
     * @param authority the user/group the permission was granted to
     * @param permission the permission that was granted
     * @param grantedBy the user who granted the permission
     * @param dateGranted when the permission was granted
     * @param expiryDate when the permission expires (can be null)
     */
    @Transactional
    public void recordPermissionGrant(NodeRef nodeRef, String authority, String permission, 
                                    String grantedBy, Date dateGranted, Date expiryDate) {
        try {
            logger.info("RECORDING PERMISSION GRANT - NodeRef: " + nodeRef + ", Authority: " + authority + 
                       ", Permission: " + permission + ", GrantedBy: " + grantedBy + 
                       ", DateGranted: " + dateGranted + ", ExpiryDate: " + expiryDate);
            
            String sql = "INSERT INTO permission_audit (node_ref, user_granted_to, granted_by, date_granted, expiry_date, permission, action_type) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'GRANT')";
            
            jdbcTemplate.update(sql, 
                nodeRef.toString(),
                authority,
                grantedBy,
                dateGranted,
                expiryDate,
                permission
            );
            
            logger.info("SUCCESSFULLY RECORDED PERMISSION GRANT in database");
        } catch (Exception e) {
            logger.error("ERROR RECORDING PERMISSION GRANT: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Record a permission revoke event
     * @param nodeRef the node the permission was revoked from
     * @param authority the user/group the permission was revoked from
     * @param permission the permission that was revoked
     * @param revokedBy the user who revoked the permission
     * @param dateRevoked when the permission was revoked
     */
    @Transactional
    public void recordPermissionRevoke(NodeRef nodeRef, String authority, String permission, 
                                     String revokedBy, Date dateRevoked) {
        try {
            logger.info("RECORDING PERMISSION REVOKE - NodeRef: " + nodeRef + ", Authority: " + authority + 
                       ", Permission: " + permission + ", RevokedBy: " + revokedBy + 
                       ", DateRevoked: " + dateRevoked);
            
            String sql = "INSERT INTO permission_audit (node_ref, user_granted_to, granted_by, date_granted, expiry_date, permission, action_type) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'REVOKE')";
            
            jdbcTemplate.update(sql, 
                nodeRef.toString(),
                authority,
                revokedBy,
                dateRevoked,
                null, // No expiry date for revokes
                permission
            );
            
            logger.info("SUCCESSFULLY RECORDED PERMISSION REVOKE in database");
        } catch (Exception e) {
            logger.error("ERROR RECORDING PERMISSION REVOKE: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get permission audit data for a specific node
     * @param nodeRef the node to get audit data for
     * @return list of permission audit entries
     */
    public List<PermissionAuditEntry> getPermissionAuditForNode(NodeRef nodeRef) {
        try {
            String sql = "SELECT * FROM permission_audit WHERE node_ref = ? ORDER BY date_granted DESC";
            
            return jdbcTemplate.query(sql, new PermissionAuditRowMapper(), nodeRef.toString());
            
        } catch (Exception e) {
            logger.error("Error getting permission audit for node " + nodeRef + ": " + e.getMessage(), e);
            return new java.util.ArrayList<PermissionAuditEntry>();
        }
    }
    
    /**
     * Get permission audit data for a specific user
     * @param username the username to get audit data for
     * @return list of permission audit entries
     */
    public List<PermissionAuditEntry> getPermissionAuditForUser(String username) {
        try {
            String sql = "SELECT * FROM permission_audit WHERE user_granted_to = ? ORDER BY date_granted DESC";
            
            return jdbcTemplate.query(sql, new PermissionAuditRowMapper(), username);
            
        } catch (Exception e) {
            logger.error("Error getting permission audit for user " + username + ": " + e.getMessage(), e);
            return new java.util.ArrayList<PermissionAuditEntry>();
        }
    }
    
    /**
     * Get the most recent permission grant for a user on a specific node
     * @param nodeRef the node
     * @param username the username
     * @param permission the permission
     * @return the most recent grant entry or null if not found
     */
    public PermissionAuditEntry getLatestPermissionGrant(NodeRef nodeRef, String username, String permission) {
        try {
            String sql = "SELECT * FROM permission_audit WHERE node_ref = ? AND user_granted_to = ? " +
                        "AND permission = ? AND action_type = 'GRANT' " +
                        "ORDER BY date_granted DESC LIMIT 1";
            
            List<PermissionAuditEntry> results = jdbcTemplate.query(sql, 
                new PermissionAuditRowMapper(), nodeRef.toString(), username, permission);
            
            return results.isEmpty() ? null : results.get(0);
            
        } catch (Exception e) {
            logger.error("Error getting latest permission grant: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if a permission has expired
     * @param nodeRef the node
     * @param username the username
     * @param permission the permission
     * @return true if the permission has expired, false otherwise
     */
    public boolean isPermissionExpired(NodeRef nodeRef, String username, String permission) {
        PermissionAuditEntry entry = getLatestPermissionGrant(nodeRef, username, permission);
        if (entry != null && entry.getExpiryDate() != null) {
            return entry.getExpiryDate().before(new Date());
        }
        return false;
    }
    
    /**
     * Get all users who have granted permissions (for reporting)
     * @return list of usernames who have granted permissions
     */
    public List<String> getAllPermissionGrantors() {
        try {
            String sql = "SELECT DISTINCT granted_by FROM permission_audit WHERE granted_by IS NOT NULL";
            
            return jdbcTemplate.queryForList(sql, String.class);
            
        } catch (Exception e) {
            logger.error("Error getting permission grantors: " + e.getMessage(), e);
            return new java.util.ArrayList<String>();
        }
    }
    
    /**
     * Row mapper for permission audit entries
     */
    private static class PermissionAuditRowMapper implements RowMapper<PermissionAuditEntry> {
        @Override
        public PermissionAuditEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            PermissionAuditEntry entry = new PermissionAuditEntry();
            entry.setId(rs.getLong("id"));
            entry.setNodeRef(rs.getString("node_ref"));
            entry.setUserGrantedTo(rs.getString("user_granted_to"));
            entry.setGrantedBy(rs.getString("granted_by"));
            entry.setDateGranted(rs.getTimestamp("date_granted"));
            entry.setExpiryDate(rs.getTimestamp("expiry_date"));
            entry.setPermission(rs.getString("permission"));
            entry.setActionType(rs.getString("action_type"));
            entry.setCreatedAt(rs.getTimestamp("created_at"));
            return entry;
        }
    }
    
    /**
     * Inner class to represent a permission audit entry
     */
    public static class PermissionAuditEntry {
        private Long id;
        private String nodeRef;
        private String userGrantedTo;
        private String grantedBy;
        private Date dateGranted;
        private Date expiryDate;
        private String permission;
        private String actionType;
        private Date createdAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getNodeRef() { return nodeRef; }
        public void setNodeRef(String nodeRef) { this.nodeRef = nodeRef; }
        
        public String getUserGrantedTo() { return userGrantedTo; }
        public void setUserGrantedTo(String userGrantedTo) { this.userGrantedTo = userGrantedTo; }
        
        public String getGrantedBy() { return grantedBy; }
        public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
        
        public Date getDateGranted() { return dateGranted; }
        public void setDateGranted(Date dateGranted) { this.dateGranted = dateGranted; }
        
        public Date getExpiryDate() { return expiryDate; }
        public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
        
        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }
        
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    }
}
