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
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.alfresco.service.cmr.security.AccessPermission;

public class DirectPermissionsWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(DirectPermissionsWebScript.class);

    private NodeService nodeService;
    private SiteService siteService;
    private PermissionService permissionService;
    private AuthorityService authorityService;

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

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, String>> permissions = new ArrayList<Map<String, String>>();
        int totalPermissions = 0;
        int userPermissions = 0;
        int groupPermissions = 0;
        int effectivePermissions = 0;

        try {
            String siteShortName = req.getParameter("site");

            if (siteShortName == null) {
                status.setCode(400);
                model.put("success", false);
                model.put("error", "Missing required parameter: site");
                return model;
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
                        
                        // Check if this is a user or group
                        if (authorityName.startsWith("GROUP_")) {
                            // This is a group permission - expand it for Phase 2
                            groupPermissions++;
                            logger.debug("Expanding group permission: " + authorityName + " on node: " + nodeRef);
                            
                            // Get all users in this group (recursive)
                            Set<String> groupUsers = getUsersInGroup(authorityName, new HashSet<String>());
                            
                            for (String groupUser : groupUsers) {
                                effectivePermissions++;
                                
                                Map<String, String> permissionEntry = new HashMap<String, String>();
                                permissionEntry.put("username", groupUser);
                                permissionEntry.put("nodePath", getNodePath(nodeRef));
                                permissionEntry.put("role", accessPermission.getPermission());
                                permissionEntry.put("nodeName", (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
                                permissionEntry.put("nodeType", nodeService.getType(nodeRef).toString());
                                permissionEntry.put("groupName", authorityName);
                                permissionEntry.put("permissionType", "GROUP");
                                
                                permissions.add(permissionEntry);
                            }
                        } else {
                            // This is a user permission - include it
                            userPermissions++;
                            
                            Map<String, String> permissionEntry = new HashMap<String, String>();
                            permissionEntry.put("username", authorityName);
                            permissionEntry.put("nodePath", getNodePath(nodeRef));
                            permissionEntry.put("role", accessPermission.getPermission());
                            permissionEntry.put("nodeName", (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
                            permissionEntry.put("nodeType", nodeService.getType(nodeRef).toString());
                            permissionEntry.put("groupName", "");
                            permissionEntry.put("permissionType", "DIRECT");
                            
                            permissions.add(permissionEntry);
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
            model.put("totalPermissions", permissions.size());
            model.put("permissions", permissions);

            logger.info("Direct permissions report for site " + siteShortName + 
                       ": " + permissions.size() + " permissions found (direct + group-based)");

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
}
