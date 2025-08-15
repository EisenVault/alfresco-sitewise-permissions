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
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class CreateTestContentWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(CreateTestContentWebScript.class);

    private NodeService nodeService;
    private SiteService siteService;
    private PermissionService permissionService;
    private AuthorityService authorityService;
    private NamespaceService namespaceService;

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

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        int createdCount = 0;
        int errorCount = 0;

        try {
            // Define test content structure (comprehensive folders for each site)
            String[][] contentStructure = {
                // Site, Folder Name, Group with permissions, Permission level
                // CRM Content
                {"crm", "Customers", "CRM_Managers", "SiteManager"},
                {"crm", "Leads", "CRM_Users", "SiteContributor"},
                {"crm", "Sales", "CRM_Sales_Team", "SiteContributor"},
                {"crm", "Marketing", "CRM_Users", "SiteContributor"},
                {"crm", "Support", "CRM_Users", "SiteContributor"},
                {"crm", "Analytics", "CRM_Admins", "SiteManager"},
                {"crm", "Reports", "CRM_Viewers", "SiteConsumer"},
                {"crm", "Templates", "CRM_Contributors", "SiteContributor"},
                
                // HR Content
                {"hr", "Employees", "HR_Managers", "SiteManager"},
                {"hr", "Policies", "HR_Users", "SiteContributor"},
                {"hr", "Recruitment", "HR_Recruitment_Team", "SiteContributor"},
                {"hr", "Training", "HR_Training_Team", "SiteContributor"},
                {"hr", "Payroll", "HR_Payroll_Team", "SiteContributor"},
                {"hr", "Benefits", "HR_Employee_Relations", "SiteContributor"},
                {"hr", "Compliance", "HR_Admins", "SiteManager"},
                {"hr", "Reports", "HR_Viewers", "SiteConsumer"},
                {"hr", "Forms", "HR_Contributors", "SiteContributor"},
                
                // Finance Content
                {"finance", "Budgets", "Finance_Managers", "SiteManager"},
                {"finance", "Reports", "Finance_Users", "SiteContributor"},
                {"finance", "Accounting", "Finance_Accounting_Team", "SiteContributor"},
                {"finance", "Audit", "Finance_Audit_Team", "SiteContributor"},
                {"finance", "Tax", "Finance_Tax_Team", "SiteContributor"},
                {"finance", "Forecasting", "Finance_Budget_Team", "SiteContributor"},
                {"finance", "Compliance", "Finance_Admins", "SiteManager"},
                {"finance", "Templates", "Finance_Contributors", "SiteContributor"},
                {"finance", "Archive", "Finance_Viewers", "SiteConsumer"}
            };

            for (String[] content : contentStructure) {
                String siteShortName = content[0];
                String folderName = content[1];
                String groupName = content[2];
                String permissionLevel = content[3];

                try {
                    // Get site document library
                    NodeRef siteRef = siteService.getSite(siteShortName).getNodeRef();
                    NodeRef docLibRef = siteService.getContainer(siteShortName, "documentLibrary");
                    
                    if (docLibRef == null) {
                        logger.warn("Document library not found for site: " + siteShortName);
                        continue;
                    }

                    // Create folder
                    QName folderType = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "folder");
                    NodeRef folderRef = nodeService.createNode(
                        docLibRef,
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "contains"),
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, folderName),
                        folderType
                    ).getChildRef();

                    // Set folder properties
                    Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
                    properties.put(QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "name"), folderName);
                    properties.put(QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "title"), folderName);
                    nodeService.setProperties(folderRef, properties);

                    // Set permissions
                    if (authorityService.authorityExists(groupName)) {
                        permissionService.setPermission(folderRef, groupName, permissionLevel, true);
                        logger.info("Set " + permissionLevel + " permission for " + groupName + " on " + folderName + " in " + siteShortName);
                    } else {
                        logger.warn("Group " + groupName + " does not exist, skipping permission setting");
                    }

                    Map<String, String> result = new HashMap<String, String>();
                    result.put("site", siteShortName);
                    result.put("folder", folderName);
                    result.put("status", "created");
                    result.put("message", "Folder created successfully with permissions");
                    results.add(result);
                    createdCount++;

                } catch (Exception e) {
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("site", siteShortName);
                    result.put("folder", folderName);
                    result.put("status", "error");
                    result.put("message", "Error creating folder: " + e.getMessage());
                    results.add(result);
                    errorCount++;
                    
                    logger.error("Error creating folder " + folderName + " in site " + siteShortName + ": " + e.getMessage(), e);
                }
            }

            model.put("message", "Test content creation completed");
            model.put("totalFolders", contentStructure.length);
            model.put("createdCount", createdCount);
            model.put("errorCount", errorCount);
            model.put("results", results);

        } catch (Exception e) {
            status.setCode(500);
            model.put("error", "Failed to create test content: " + e.getMessage());
            logger.error("Error creating test content: " + e.getMessage(), e);
        }

        return model;
    }
}
