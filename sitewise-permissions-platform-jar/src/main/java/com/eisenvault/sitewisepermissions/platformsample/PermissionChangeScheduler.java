package com.eisenvault.sitewisepermissions.platformsample;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Comprehensive permission change checker for Alfresco 5.2.
 * This is a reliable approach since OnUpdateNodePolicy may not fire on permission changes.
 * Provides both single-node and site-wide permission checking capabilities.
 */
public class PermissionChangeScheduler {
    
    private static final Log logger = LogFactory.getLog(PermissionChangeScheduler.class);
    
    private SiteService siteService;
    private NodeService nodeService;
    private PermissionService permissionService;
    private PermissionAuditService permissionAuditService;
    
    // Track which nodes we've already processed to avoid reprocessing
    private final Set<NodeRef> processedNodes = new HashSet<>();
    
    // Track the last known permissions for each node to detect removals
    private final Map<NodeRef, Set<String>> lastKnownPermissions = new HashMap<>();
    
    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }
    
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
    public void setPermissionAuditService(PermissionAuditService permissionAuditService) {
        this.permissionAuditService = permissionAuditService;
    }
    
    /**
     * Check permissions for a specific node and detect changes
     */
    public void checkNodePermissions(NodeRef nodeRef) {
        try {
            logger.debug("Checking permissions for node: " + nodeRef);
            
            Date now = new Date();
            
            // Check permissions for this specific node
            checkNodePermissionsInternal(nodeRef, now);
            
        } catch (Exception e) {
            logger.error("Error checking node permissions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Comprehensive method to check for permission changes across ALL nodes in the repository
     * This is more reliable than OnUpdateNodePolicy for permission changes
     */
    public void checkPermissionChanges() {
        try {
            logger.info("=== PERMISSION CHANGE CHECKER STARTED ===");
            
            // Clear the processed nodes cache to ensure we check all nodes
            processedNodes.clear();
            
            // Initialize last known permissions from database if this is the first run
            if (lastKnownPermissions.isEmpty()) {
                initializeLastKnownPermissions();
            }
            
            Date now = new Date();
            int newPermissionsFound = 0;
            
            // Check all sites using Alfresco 5.2 compatible API
            List<SiteInfo> sites = siteService.listSites("", "", 1000);
            logger.info("Found " + sites.size() + " sites to check");
            
            for (SiteInfo site : sites) {
                try {
                    NodeRef siteNodeRef = site.getNodeRef();
                    NodeRef documentLibrary = siteService.getContainer(site.getShortName(), "documentLibrary");
                    
                    // Check site node
                    if (nodeService.exists(siteNodeRef)) {
                        int sitePermissions = checkNodePermissionsInternal(siteNodeRef, now);
                        newPermissionsFound += sitePermissions;
                    }
                    
                    // Check document library
                    if (documentLibrary != null && nodeService.exists(documentLibrary)) {
                        int docLibPermissions = checkNodePermissionsInternal(documentLibrary, now);
                        newPermissionsFound += docLibPermissions;
                    }
                    
                    // Check all nodes in document library recursively
                    if (documentLibrary != null && nodeService.exists(documentLibrary)) {
                        int childPermissions = checkChildNodes(documentLibrary, now);
                        newPermissionsFound += childPermissions;
                    }
                    
                } catch (Exception e) {
                    logger.error("Error checking site " + site.getShortName() + ": " + e.getMessage(), e);
                }
            }
            
            // Check ALL nodes in document libraries comprehensively
            logger.info("Completed site-level permission checking. All document library nodes have been checked recursively.");
            
            logger.info("=== PERMISSION CHANGE CHECKER COMPLETED ===");
            logger.info("Total nodes checked: " + processedNodes.size());
            logger.info("New permissions found: " + newPermissionsFound);
            
        } catch (Exception e) {
            logger.error("Error in permission change checker: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize the last known permissions map from the database
     */
    private void initializeLastKnownPermissions() {
        try {
            logger.info("Initializing last known permissions from database...");
            
            // Get all active permissions from the database
            String sql = "SELECT node_ref, user_granted_to, permission FROM permission_audit WHERE is_active = TRUE";
            
            List<Map<String, Object>> results = permissionAuditService.getJdbcTemplate().queryForList(sql);
            
            for (Map<String, Object> row : results) {
                String nodeRefStr = (String) row.get("node_ref");
                String authority = (String) row.get("user_granted_to");
                String permission = (String) row.get("permission");
                
                NodeRef nodeRef = new NodeRef(nodeRefStr);
                String permKey = authority + ":" + permission;
                
                if (!lastKnownPermissions.containsKey(nodeRef)) {
                    lastKnownPermissions.put(nodeRef, new HashSet<String>());
                }
                lastKnownPermissions.get(nodeRef).add(permKey);
            }
            
            logger.info("Initialized last known permissions for " + lastKnownPermissions.size() + " nodes");
            
        } catch (Exception e) {
            logger.error("Error initializing last known permissions: " + e.getMessage(), e);
        }
    }
    

    
    /**
     * Check permissions for a specific node (internal method)
     */
    private int checkNodePermissionsInternal(NodeRef nodeRef, Date now) {
        int newPermissionsFound = 0;
        int revokedPermissionsFound = 0;
        
        try {
            // Skip if we've already processed this node recently
            if (processedNodes.contains(nodeRef)) {
                return 0;
            }
            
            // Add to processed set
            processedNodes.add(nodeRef);
            
            // Check if this node has any permissions set
            Set<AccessPermission> currentPerms = permissionService.getAllSetPermissions(nodeRef);
            
            // Get the last known permissions for this node
            Set<String> lastKnownPerms = lastKnownPermissions.get(nodeRef);
            if (lastKnownPerms == null) {
                lastKnownPerms = new HashSet<>();
            }
            
            // Track current permissions for comparison
            Set<String> currentPermKeys = new HashSet<>();
            
            for (AccessPermission currentPerm : currentPerms) {
                // Skip system permissions
                if (PermissionUtils.isSystemPermission(currentPerm.getAuthority(), currentPerm.getPermission())) {
                    continue;
                }
                
                String permKey = currentPerm.getAuthority() + ":" + currentPerm.getPermission();
                currentPermKeys.add(permKey);
                
                // Check if this permission was already recorded in the database
                PermissionAuditService.PermissionAuditEntry existingEntry = permissionAuditService.getLatestPermissionGrant(
                    nodeRef, currentPerm.getAuthority(), currentPerm.getPermission());
                
                if (existingEntry == null) {
                    // This is a new permission - record it
                    logger.info("[PERMISSION ADDED] Node: " + nodeRef +
                               " | Authority: " + currentPerm.getAuthority() +
                               " | Permission: " + currentPerm.getPermission());
                    
                    // Record in audit table
                    permissionAuditService.recordPermissionGrant(nodeRef, currentPerm.getAuthority(), 
                        currentPerm.getPermission(), now, null);
                    
                    newPermissionsFound++;
                }
            }
            
            // Check for revoked permissions (permissions that existed before but not now)
            for (String lastPermKey : lastKnownPerms) {
                if (!currentPermKeys.contains(lastPermKey)) {
                    // This permission was revoked
                    String[] parts = lastPermKey.split(":", 2);
                    if (parts.length == 2) {
                        String authority = parts[0];
                        String permission = parts[1];
                        
                        logger.info("[PERMISSION REVOKED] Node: " + nodeRef +
                                   " | Authority: " + authority +
                                   " | Permission: " + permission);
                        
                        // Record the revocation
                        permissionAuditService.recordPermissionRevoke(nodeRef, authority, permission, now);
                        revokedPermissionsFound++;
                    }
                }
            }
            
            // Update the last known permissions for this node
            lastKnownPermissions.put(nodeRef, currentPermKeys);
            
        } catch (Exception e) {
            logger.error("Error checking permissions for node " + nodeRef + ": " + e.getMessage(), e);
        }
        
        return newPermissionsFound + revokedPermissionsFound;
    }
    
    /**
     * Recursively check ALL child nodes (both files and folders)
     */
    private int checkChildNodes(NodeRef parentNode, Date now) {
        int totalNewPermissions = 0;
        int nodesChecked = 0;
        
        try {
            List<org.alfresco.service.cmr.repository.ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentNode);
            logger.debug("Found " + childAssocs.size() + " child nodes under " + parentNode);
            
            for (org.alfresco.service.cmr.repository.ChildAssociationRef assoc : childAssocs) {
                NodeRef childNode = assoc.getChildRef();
                
                if (nodeService.exists(childNode)) {
                    nodesChecked++;
                    
                    // Check permissions for this child node (file or folder)
                    int childPermissions = checkNodePermissionsInternal(childNode, now);
                    totalNewPermissions += childPermissions;
                    
                    // Recursively check ALL children (both files and folders)
                    // This ensures we check every single node in the document library
                    int grandChildPermissions = checkChildNodes(childNode, now);
                    totalNewPermissions += grandChildPermissions;
                }
            }
            
            if (nodesChecked > 0) {
                logger.debug("Checked " + nodesChecked + " child nodes under " + parentNode + ", found " + totalNewPermissions + " new permissions");
            }
            
        } catch (Exception e) {
            logger.error("Error checking child nodes for " + parentNode + ": " + e.getMessage(), e);
        }
        
        return totalNewPermissions;
    }
    

    
    /**
     * Clear the processed nodes cache (useful for testing)
     */
    public void clearProcessedNodesCache() {
        processedNodes.clear();
        logger.info("Cleared processed nodes cache");
    }
    
    /**
     * Manually trigger permission checking
     */
    public void manualCheck() {
        logger.info("Manual permission check triggered");
        checkPermissionChanges();
    }
}
