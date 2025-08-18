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

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.alfresco.service.cmr.security.AccessPermission;

public class DirectPermissionsXlsxWebScript extends AbstractWebScript {
    private static Log logger = LogFactory.getLog(DirectPermissionsXlsxWebScript.class);

    private NodeService nodeService;
    private SiteService siteService;
    private PermissionService permissionService;
    private AuthorityService authorityService;
    private PersonService personService;
    private LoginAuditService loginAuditService;
    private PermissionAuditService permissionAuditService; // Added for Phase 5
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

    public void execute(WebScriptRequest req, WebScriptResponse res) {
        try {
            String siteShortName = req.getParameter("site");

            if (siteShortName == null) {
                res.setStatus(400);
                res.setContentType("application/json");
                res.getWriter().write("{\"success\":false,\"error\":\"Missing required parameter: site\"}");
                return;
            }

            NodeRef siteNodeRef = siteService.getSite(siteShortName).getNodeRef();
            if (siteNodeRef == null) {
                res.setStatus(404);
                res.setContentType("application/json");
                res.getWriter().write("{\"success\":false,\"error\":\"Site " + siteShortName + " not found\"}");
                return;
            }

            NodeRef documentLibrary = siteService.getContainer(siteShortName, "documentLibrary");
            if (documentLibrary == null) {
                res.setStatus(404);
                res.setContentType("application/json");
                res.getWriter().write("{\"success\":false,\"error\":\"Document Library not found in site " + siteShortName + "\"}");
                return;
            }

            // Generate XLSX file
            res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            res.setHeader("Content-Disposition", "attachment; filename=permissions_" + siteShortName + ".xlsx");
            
            try (OutputStream out = res.getOutputStream()) {
                generateXlsxFile(siteShortName, documentLibrary, out);
            }

        } catch (Exception e) {
            logger.error("Error in XLSX web script: " + e.getMessage(), e);
            try {
                res.setStatus(500);
                res.setContentType("application/json");
                res.getWriter().write("{\"success\":false,\"error\":\"Internal server error\"}");
            } catch (Exception ex) {
                logger.error("Error writing error response: " + ex.getMessage(), ex);
            }
        }
    }

    private void generateXlsxFile(String siteShortName, NodeRef documentLibrary, OutputStream out) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Permissions");
        
        // Create header style
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        // Create data style
        XSSFCellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
                // Create headers
        String[] headers = {"Username", "Site", "Node Name", "Current Role / Permission Status", 
                           "From Date", "User Status", "User Login", "Group Name", "NodeRef", "Node Type", "Document Path"};
        
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Get permissions data
        List<Map<String, String>> permissions = getPermissionsData(siteShortName, documentLibrary);
        
        // Add data rows
        int rowNum = 1;
        for (Map<String, String> permission : permissions) {
            XSSFRow row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(permission.get("username"));
            row.createCell(1).setCellValue(permission.get("site"));
            row.createCell(2).setCellValue(permission.get("nodeName"));
            row.createCell(3).setCellValue(permission.get("currentRole"));
            row.createCell(4).setCellValue(permission.get("fromDate"));
            row.createCell(5).setCellValue(permission.get("userStatus"));
            row.createCell(6).setCellValue(permission.get("userLogin"));
            row.createCell(7).setCellValue(permission.get("groupName"));
            row.createCell(8).setCellValue(permission.get("nodeRef"));
            row.createCell(9).setCellValue(permission.get("nodeType"));
            row.createCell(10).setCellValue(permission.get("documentPath"));
            
            // Apply data style to all cells
            for (int i = 0; i < 11; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        workbook.write(out);
        // Don't call close() on workbook as it's not available in all POI versions
        // The workbook will be garbage collected
        
        logger.info("XLSX permissions report for site " + siteShortName + 
                   ": " + permissions.size() + " permissions found (direct + group-based)");
    }

    private List<Map<String, String>> getPermissionsData(String siteShortName, NodeRef documentLibrary) {
        List<Map<String, String>> permissions = new ArrayList<Map<String, String>>();
        List<NodeRef> allNodes = getAllNodesInContainer(documentLibrary);
        
        for (NodeRef nodeRef : allNodes) {
            try {
                Set<AccessPermission> setPermissions = permissionService.getAllSetPermissions(nodeRef);
                
                for (AccessPermission accessPermission : setPermissions) {
                    String authorityName = accessPermission.getAuthority();
                    
                    // Skip GROUP_EVERYONE as it causes issues and is not useful for reporting
                    if ("GROUP_EVERYONE".equals(authorityName)) {
                        continue;
                    }
                    
                    if (authorityName.startsWith("GROUP_")) {
                        Set<String> groupUsers = getUsersInGroup(authorityName, new HashSet<String>());
                        
                        for (String groupUser : groupUsers) {
                            Map<String, String> permissionEntry = createPermissionEntry(
                                groupUser, siteShortName, nodeRef, accessPermission, 
                                authorityName, "GROUP"
                            );
                            permissions.add(permissionEntry);
                        }
                    } else {
                        Map<String, String> permissionEntry = createPermissionEntry(
                            authorityName, siteShortName, nodeRef, accessPermission, 
                            "", "DIRECT"
                        );
                        permissions.add(permissionEntry);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error processing permissions for node " + nodeRef + ": " + e.getMessage());
            }
        }
        
        return permissions;
    }

    private Map<String, String> createPermissionEntry(String username, String site, NodeRef nodeRef, 
                                                     AccessPermission accessPermission, String groupName, String permissionType) {
        Map<String, String> entry = new HashMap<String, String>();
        
        entry.put("username", username);
        entry.put("site", site);
        entry.put("nodeRef", nodeRef.toString());
        entry.put("nodeName", getNodeName(nodeRef));
        entry.put("nodeType", getNodeType(nodeRef));
        entry.put("documentPath", getNodePath(nodeRef));
        entry.put("currentRole", getRoleDisplayName(accessPermission.getPermission()));
        entry.put("fromDate", getPermissionFromDate(nodeRef, accessPermission));
        entry.put("userStatus", getUserStatus(username));
        entry.put("userLogin", getLastLoginDate(username));
        entry.put("groupName", groupName);
        
        return entry;
    }

    private String getRoleDisplayName(String permission) {
        switch (permission) {
            case "SiteManager": return "Manager";
            case "SiteCollaborator": return "Collaborator";
            case "SiteContributor": return "Contributor";
            case "SiteConsumer": return "Consumer";
            case "Read": return "Viewer";
            case "Write": return "Editor";
            case "Delete": return "Manager";
            case "CreateChildren": return "Contributor";
            default: return permission;
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

    private List<NodeRef> getAllNodesInContainer(NodeRef container) {
        List<NodeRef> allNodes = new ArrayList<NodeRef>();
        allNodes.add(container);
        
        List<org.alfresco.service.cmr.repository.ChildAssociationRef> childAssocs = nodeService.getChildAssocs(container);
        for (org.alfresco.service.cmr.repository.ChildAssociationRef assoc : childAssocs) {
            NodeRef child = assoc.getChildRef();
            allNodes.add(child);
            
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
    
    private String getNodeName(NodeRef nodeRef) {
        try {
            if (nodeService.exists(nodeRef)) {
                Object nameProp = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
                if (nameProp != null) {
                    return nameProp.toString();
                }
            }
            return "Unknown";
        } catch (Exception e) {
            logger.warn("Could not get name for node " + nodeRef + ": " + e.getMessage());
            return "Unknown";
        }
    }
    
    private String getNodeType(NodeRef nodeRef) {
        try {
            if (nodeService.exists(nodeRef)) {
                QName type = nodeService.getType(nodeRef);
                if (type != null) {
                    return type.toString();
                }
            }
            return "Unknown";
        } catch (Exception e) {
            logger.warn("Could not get type for node " + nodeRef + ": " + e.getMessage());
            return "Unknown";
        }
    }

    private Set<String> getUsersInGroup(String groupName, Set<String> visitedGroups) {
        Set<String> users = new HashSet<String>();
        
        if (visitedGroups.contains(groupName)) {
            logger.warn("Circular group reference detected: " + groupName);
            return users;
        }
        
        visitedGroups.add(groupName);
        
        try {
            Set<String> containedAuthorities = authorityService.getContainedAuthorities(null, groupName, true);
            logger.debug("Group " + groupName + " contains " + containedAuthorities.size() + " authorities");
            
            for (String authority : containedAuthorities) {
                if (authority.startsWith("GROUP_")) {
                    Set<String> nestedUsers = getUsersInGroup(authority, visitedGroups);
                    users.addAll(nestedUsers);
                } else {
                    users.add(authority);
                }
            }
        } catch (Exception e) {
            logger.warn("Error expanding group " + groupName + ": " + e.getMessage());
        }
        
        return users;
    }
}
