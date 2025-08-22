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

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import org.alfresco.service.cmr.security.AccessPermission;

public class DirectPermissionsWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(DirectPermissionsWebScript.class);

    private NodeService nodeService;
    private SiteService siteService;
    private PermissionService permissionService;
    private AuthorityService authorityService;
    private PersonService personService;
    private LoginAuditService loginAuditService;
    private PermissionAuditService permissionAuditService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setLoginAuditService(LoginAuditService loginAuditService) {
        this.loginAuditService = loginAuditService;
    }

    public void setPermissionAuditService(PermissionAuditService permissionAuditService) {
        this.permissionAuditService = permissionAuditService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, String>> permissions = new ArrayList<Map<String, String>>();
        int totalPermissions = 0;
        int userPermissions = 0;
        int groupPermissions = 0;
        int effectivePermissions = 0;
        int filteredPermissions = 0;

        try {
            // Parse filter parameters
            String siteShortName = req.getParameter("site");
            String userStatusFilter = req.getParameter("userStatus"); // All, Active, Inactive
            String fromDateFilter = req.getParameter("fromDate"); // yyyy-MM-dd format
            String usernameSearch = req.getParameter("usernameSearch"); // partial match

            if (siteShortName == null) {
                status.setCode(400);
                model.put("success", false);
                model.put("error", "Missing required parameter: site");
                return model;
            }

            // Validate user status filter
            if (userStatusFilter != null && !userStatusFilter.isEmpty() && 
                !userStatusFilter.equals("All") && !userStatusFilter.equals("Active") && !userStatusFilter.equals("Inactive")) {
                status.setCode(400);
                model.put("success", false);
                model.put("error", "Invalid userStatus parameter. Must be 'All', 'Active', or 'Inactive'");
                return model;
            }

            // Parse from date filter
            Date fromDate = null;
            if (fromDateFilter != null && !fromDateFilter.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    fromDate = sdf.parse(fromDateFilter);
                } catch (ParseException e) {
                    status.setCode(400);
                    model.put("success", false);
                    model.put("error", "Invalid fromDate parameter. Must be in yyyy-MM-dd format");
                    return model;
                }
            }

            NodeRef siteNodeRef = siteService.getSite(siteShortName).getNodeRef();
            if (siteNodeRef == null) {
                status.setCode(404);
                model.put("success", false);
                model.put("error", "Site " + siteShortName + " not found");
                return model;
            }

            NodeRef documentLibrary = siteService.getContainer(siteShortName, "documentLibrary");
            if (documentLibrary == null) {
                status.setCode(404);
                model.put("success", false);
                model.put("error", "Document Library not found in site " + siteShortName);
                return model;
            }

            // Get all nodes in the document library recursively
            List<NodeRef> allNodes = getAllNodesInContainer(documentLibrary);
            logger.info("Found " + allNodes.size() + " nodes in site " + siteShortName);

            for (NodeRef nodeRef : allNodes) {
                try {
                    // Get all set permissions for this node
                    Set<AccessPermission> setPermissions = permissionService.getAllSetPermissions(nodeRef);
                    
                    for (AccessPermission accessPermission : setPermissions) {
                        totalPermissions++;
                        String authorityName = accessPermission.getAuthority();
                        
                        // Skip GROUP_EVERYONE as it causes issues and is not useful for reporting
                        if ("GROUP_EVERYONE".equals(authorityName)) {
                            continue;
                        }
                        
                        // Check if this is a user or group
                        if (authorityName.startsWith("GROUP_")) {
                            // This is a group permission - expand it for Phase 2
                            groupPermissions++;
                            logger.debug("Expanding group permission: " + authorityName + " on node: " + nodeRef);
                            
                            // Get all users in this group (recursive)
                            Set<String> groupUsers = getUsersInGroup(authorityName, new HashSet<String>());
                            
                            for (String groupUser : groupUsers) {
                                effectivePermissions++;
                                
                                // Apply filters
                                if (shouldIncludePermission(groupUser, nodeRef, accessPermission, userStatusFilter, fromDate, usernameSearch)) {
                                    filteredPermissions++;
                                    
                                                                    Map<String, String> permissionEntry = new HashMap<String, String>();
                                permissionEntry.put("username", groupUser);
                                permissionEntry.put("nodePath", getNodePath(nodeRef));
                                permissionEntry.put("role", accessPermission.getPermission());
                                permissionEntry.put("nodeName", (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
                                permissionEntry.put("nodeType", nodeService.getType(nodeRef).toString());
                                permissionEntry.put("groupName", authorityName);
                                permissionEntry.put("permissionType", "GROUP");
                                
                                permissionEntry.put("site", siteShortName);
                                permissionEntry.put("nodeRef", nodeRef.toString());
                                permissionEntry.put("fromDate", getPermissionFromDate(nodeRef, accessPermission));
                                permissionEntry.put("userStatus", getUserStatus(groupUser));
                                permissionEntry.put("userLogin", getLastLoginDate(groupUser));
                                    
                                    permissions.add(permissionEntry);
                                }
                            }
                        } else {
                            // This is a user permission - include it
                            userPermissions++;
                            
                            // Apply filters
                            if (shouldIncludePermission(authorityName, nodeRef, accessPermission, userStatusFilter, fromDate, usernameSearch)) {
                                filteredPermissions++;
                                
                                Map<String, String> permissionEntry = new HashMap<String, String>();
                                permissionEntry.put("username", authorityName);
                                permissionEntry.put("nodePath", getNodePath(nodeRef));
                                permissionEntry.put("role", accessPermission.getPermission());
                                permissionEntry.put("nodeName", (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
                                permissionEntry.put("nodeType", nodeService.getType(nodeRef).toString());
                                permissionEntry.put("groupName", "");
                                permissionEntry.put("permissionType", "DIRECT");
                                
                                permissionEntry.put("site", siteShortName);
                                permissionEntry.put("nodeRef", nodeRef.toString());
                                permissionEntry.put("fromDate", getPermissionFromDate(nodeRef, accessPermission));
                                permissionEntry.put("userStatus", getUserStatus(authorityName));
                                permissionEntry.put("userLogin", getLastLoginDate(authorityName));
                                
                                permissions.add(permissionEntry);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error processing permissions for node " + nodeRef + ": " + e.getMessage());
                }
            }

            model.put("success", true);
            model.put("site", siteShortName);
            model.put("totalNodes", allNodes.size());
            model.put("totalPermissions", totalPermissions);
            model.put("userPermissions", userPermissions);
            model.put("groupPermissions", groupPermissions);
            model.put("effectivePermissions", effectivePermissions);
            model.put("filteredPermissions", filteredPermissions);
            model.put("permissions", permissions);
            
            // Add filter information to response
            Map<String, Object> filters = new HashMap<String, Object>();
            filters.put("userStatus", userStatusFilter != null ? userStatusFilter : "All");
            filters.put("fromDate", fromDateFilter);
            filters.put("usernameSearch", usernameSearch);
            model.put("appliedFilters", filters);

            logger.info("Direct permissions report for site " + siteShortName + 
                       ": " + permissions.size() + " permissions found after filtering (direct + group-based)");

        } catch (Exception e) {
            status.setCode(500);
            model.put("success", false);
            model.put("error", "Failed to get direct permissions: " + e.getMessage());
            logger.error("Error in DirectPermissionsWebScript: " + e.getMessage(), e);
        }

        return model;
    }

    private List<NodeRef> getAllNodesInContainer(NodeRef container) {
        List<NodeRef> allNodes = new ArrayList<NodeRef>();
        allNodes.add(container); // Include the container itself
        
        List<org.alfresco.service.cmr.repository.ChildAssociationRef> childAssocs = nodeService.getChildAssocs(container);
        for (org.alfresco.service.cmr.repository.ChildAssociationRef assoc : childAssocs) {
            NodeRef child = assoc.getChildRef();
            allNodes.add(child);
            
            // Recursively get all children
            if (nodeService.getType(child).equals(ContentModel.TYPE_FOLDER)) {
                allNodes.addAll(getAllNodesInContainer(child));
            }
        }
        
        return allNodes;
    }

    private String getNodePath(NodeRef nodeRef) {
        try {
            Path path = nodeService.getPath(nodeRef);
            return path.toDisplayPath(nodeService, permissionService);
        } catch (Exception e) {
            logger.warn("Could not get path for node " + nodeRef + ": " + e.getMessage());
            return "Unknown Path";
        }
    }

    private Set<String> getUsersInGroup(String groupName, Set<String> visitedGroups) {
        Set<String> users = new HashSet<String>();
        
        // Avoid infinite loops by tracking visited groups
        if (visitedGroups.contains(groupName)) {
            logger.warn("Circular group reference detected: " + groupName);
            return users;
        }
        
        visitedGroups.add(groupName);
        
        try {
            // Get all authorities in this group
            Set<String> containedAuthorities = authorityService.getContainedAuthorities(null, groupName, true);
            logger.debug("Group " + groupName + " contains " + containedAuthorities.size() + " authorities");
            
            for (String authority : containedAuthorities) {
                if (authority.startsWith("GROUP_")) {
                    // This is a nested group - recurse
                    Set<String> nestedUsers = getUsersInGroup(authority, visitedGroups);
                    users.addAll(nestedUsers);
                } else {
                    // This is a user - add to the set
                    users.add(authority);
                }
            }
        } catch (Exception e) {
            logger.warn("Error expanding group " + groupName + ": " + e.getMessage());
        }
        
        return users;
    }

    /**
     * Check if a permission should be included based on the applied filters
     */
    private boolean shouldIncludePermission(String username, NodeRef nodeRef, AccessPermission accessPermission, 
                                          String userStatusFilter, Date fromDate, String usernameSearch) {
        try {
            // Username search filter
            if (usernameSearch != null && !usernameSearch.isEmpty()) {
                if (!username.toLowerCase().contains(usernameSearch.toLowerCase())) {
                    // Also check email if available
                    String userEmail = getUserEmail(username);
                    if (userEmail == null || !userEmail.toLowerCase().contains(usernameSearch.toLowerCase())) {
                        return false;
                    }
                }
            }

            // User status filter
            if (userStatusFilter != null && !userStatusFilter.isEmpty() && !userStatusFilter.equals("All")) {
                String userStatus = getUserStatus(username);
                if (!userStatusFilter.equals(userStatus)) {
                    return false;
                }
            }

            // From date filter
            if (fromDate != null) {
                Date permissionDate = getPermissionDate(nodeRef, accessPermission);
                if (permissionDate != null && permissionDate.before(fromDate)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.warn("Error applying filters for user " + username + ": " + e.getMessage());
            return true; // Include if filter fails
        }
    }

    /**
     * Get user email address
     */
    private String getUserEmail(String username) {
        try {
            if (personService.personExists(username)) {
                NodeRef personNode = personService.getPerson(username);
                if (personNode != null && nodeService.exists(personNode)) {
                    Object emailProp = nodeService.getProperty(personNode, ContentModel.PROP_EMAIL);
                    if (emailProp != null) {
                        return emailProp.toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            logger.warn("Error getting email for user " + username + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get user status (Active/Inactive)
     */
    private String getUserStatus(String username) {
        try {
            if (personService.personExists(username)) {
                NodeRef personNode = personService.getPerson(username);
                if (personNode != null && nodeService.exists(personNode)) {
                    // For now, assume all users are active since Alfresco doesn't expose disabled status easily
                    // In a real implementation, you would check the user's enabled status
                    return "Active";
                }
            }
            return "Unknown";
        } catch (Exception e) {
            logger.warn("Error getting user status for " + username + ": " + e.getMessage());
            return "Error";
        }
    }

    /**
     * Get permission grant date
     */
    private Date getPermissionDate(NodeRef nodeRef, AccessPermission accessPermission) {
        try {
            // Try to get from permission audit service first
            if (permissionAuditService != null) {
                PermissionAuditService.PermissionAuditEntry entry = permissionAuditService.getLatestPermissionGrant(
                    nodeRef, accessPermission.getAuthority(), accessPermission.getPermission());
                if (entry != null && entry.getDateGranted() != null) {
                    return entry.getDateGranted();
                }
            }
            
            // Fallback to node creation date
            Object createdProp = nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
            if (createdProp != null) {
                return (Date) createdProp;
            }
            return null;
        } catch (Exception e) {
            logger.warn("Error getting permission date: " + e.getMessage());
            return null;
        }
    }

    private String getPermissionFromDate(NodeRef nodeRef, AccessPermission accessPermission) {
        try {
            // Try to get from permission audit service first
            if (permissionAuditService != null) {
                PermissionAuditService.PermissionAuditEntry entry = permissionAuditService.getLatestPermissionGrant(
                    nodeRef, accessPermission.getAuthority(), accessPermission.getPermission());
                if (entry != null && entry.getDateGranted() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(entry.getDateGranted());
                }
            }
            
            // Fallback to node creation date
            Object createdProp = nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
            if (createdProp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.format((Date) createdProp);
            }
            return "Unknown";
        } catch (Exception e) {
            logger.warn("Error getting permission from date: " + e.getMessage());
            return "Unknown";
        }
    }

    private String getLastLoginDate(String username) {
        try {
            // Try to get login date from audit service first
            if (loginAuditService != null) {
                String auditLoginDate = loginAuditService.getLastLoginDate(username);
                if (auditLoginDate != null && !auditLoginDate.contains("not available") && !auditLoginDate.contains("No login audit data")) {
                    return auditLoginDate;
                }
            }
            
            // Fallback to user creation date
            if (personService.personExists(username)) {
                NodeRef personNode = personService.getPerson(username);
                if (personNode != null && nodeService.exists(personNode)) {
                    Object createdProp = nodeService.getProperty(personNode, ContentModel.PROP_CREATED);
                    if (createdProp != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        return sdf.format((Date) createdProp) + " (user created)";
                    }
                }
            }
            
            return "No login data available";
            
        } catch (Exception e) {
            logger.warn("Error getting last login for " + username + ": " + e.getMessage());
            return "Error retrieving login data";
        }
    }

}
