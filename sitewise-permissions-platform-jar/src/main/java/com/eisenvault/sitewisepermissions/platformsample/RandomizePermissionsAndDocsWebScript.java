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
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class RandomizePermissionsAndDocsWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(RandomizePermissionsAndDocsWebScript.class);

    private NodeService nodeService;
    private SiteService siteService;
    private PermissionService permissionService;
    private AuthorityService authorityService;
    private ContentService contentService;
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

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        int foldersProcessed = 0;
        int docsCreated = 0;
        int permissionsApplied = 0;
        int errorCount = 0;

        try {
            String siteShortName = req.getParameter("site");
            String docsPerFolderMinStr = req.getParameter("docsPerFolderMin");
            String docsPerFolderMaxStr = req.getParameter("docsPerFolderMax");

            if (siteShortName == null) {
                status.setCode(400);
                model.put("success", false);
                model.put("error", "Missing required parameter: site");
                return model;
            }

            int docsPerFolderMin = 2;
            int docsPerFolderMax = 5;

            if (docsPerFolderMinStr != null) {
                try {
                    docsPerFolderMin = Integer.parseInt(docsPerFolderMinStr);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid docsPerFolderMin parameter, using default: 2");
                }
            }

            if (docsPerFolderMaxStr != null) {
                try {
                    docsPerFolderMax = Integer.parseInt(docsPerFolderMaxStr);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid docsPerFolderMax parameter, using default: 5");
                }
            }

            if (docsPerFolderMin > docsPerFolderMax) {
                docsPerFolderMin = docsPerFolderMax;
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

            List<NodeRef> folders = getFoldersInContainer(documentLibrary);
            Random random = new Random();

            for (NodeRef folder : folders) {
                try {
                    String folderName = (String) nodeService.getProperty(folder, ContentModel.PROP_NAME);
                    
                    Map<String, String> folderResult = new HashMap<String, String>();
                    folderResult.put("folder", folderName);
                    folderResult.put("status", "processing");
                    results.add(folderResult);

                    applyRandomPermissionsToNode(folder, random);
                    permissionsApplied++;

                    int docsToCreate = random.nextInt(docsPerFolderMax - docsPerFolderMin + 1) + docsPerFolderMin;
                    for (int i = 0; i < docsToCreate; i++) {
                        try {
                            String docName = generateRandomDocName(siteShortName, folderName, i + 1);
                            NodeRef docNodeRef = createDummyPdf(folder, docName);
                            applyRandomPermissionsToNode(docNodeRef, random);
                            docsCreated++;
                            
                            Map<String, String> docResult = new HashMap<String, String>();
                            docResult.put("document", docName);
                            docResult.put("folder", folderName);
                            docResult.put("status", "created");
                            results.add(docResult);
                        } catch (Exception e) {
                            logger.error("Error creating document in folder " + folderName + ": " + e.getMessage(), e);
                            errorCount++;
                        }
                    }

                    foldersProcessed++;
                    folderResult.put("status", "completed");
                    folderResult.put("docsCreated", String.valueOf(docsToCreate));

                } catch (Exception e) {
                    logger.error("Error processing folder: " + e.getMessage(), e);
                    errorCount++;
                }
            }

            model.put("success", true);
            model.put("site", siteShortName);
            model.put("foldersProcessed", foldersProcessed);
            model.put("docsCreated", docsCreated);
            model.put("permissionsApplied", permissionsApplied);
            model.put("errors", errorCount);
            model.put("results", results);

            logger.info("Randomized permissions and docs for site " + siteShortName + 
                       ": " + foldersProcessed + " folders, " + docsCreated + " docs, " + 
                       permissionsApplied + " permissions applied");

        } catch (Exception e) {
            status.setCode(500);
            model.put("success", false);
            model.put("error", "Failed to randomize permissions and docs: " + e.getMessage());
            logger.error("Error in RandomizePermissionsAndDocsWebScript: " + e.getMessage(), e);
        }

        return model;
    }

    private List<NodeRef> getFoldersInContainer(NodeRef container) {
        List<NodeRef> folders = new ArrayList<NodeRef>();
        
        List<org.alfresco.service.cmr.repository.ChildAssociationRef> childAssocs = nodeService.getChildAssocs(container);
        for (org.alfresco.service.cmr.repository.ChildAssociationRef assoc : childAssocs) {
            NodeRef child = assoc.getChildRef();
            if (nodeService.getType(child).equals(ContentModel.TYPE_FOLDER)) {
                folders.add(child);
            }
        }
        
        return folders;
    }

    private void applyRandomPermissionsToNode(NodeRef nodeRef, Random random) {
        String[] permissions = {"Read", "Write", "Delete", "CreateChildren"};
        String[] authorities = getRandomAuthorities(random);
        
        for (String authority : authorities) {
            String permission = permissions[random.nextInt(permissions.length)];
            try {
                permissionService.setPermission(nodeRef, authority, permission, true);
            } catch (Exception e) {
                logger.warn("Failed to set permission " + permission + " for " + authority + " on node: " + e.getMessage());
            }
        }
    }

    private String[] getRandomAuthorities(Random random) {
        List<String> allAuthorities = new ArrayList<String>();
        
        allAuthorities.add("GROUP_CRM_Managers");
        allAuthorities.add("GROUP_CRM_Users");
        allAuthorities.add("GROUP_CRM_Sales_Team");
        allAuthorities.add("GROUP_HR_Managers");
        allAuthorities.add("GROUP_HR_Users");
        allAuthorities.add("GROUP_HR_Recruitment_Team");
        allAuthorities.add("GROUP_Finance_Managers");
        allAuthorities.add("GROUP_Finance_Users");
        allAuthorities.add("GROUP_Finance_Accounting_Team");
        
        int numAuthorities = random.nextInt(3) + 1;
        String[] selectedAuthorities = new String[numAuthorities];
        
        for (int i = 0; i < numAuthorities; i++) {
            selectedAuthorities[i] = allAuthorities.get(random.nextInt(allAuthorities.size()));
        }
        
        return selectedAuthorities;
    }

    private String generateRandomDocName(String site, String folder, int index) {
        String[] docTypes = {"report", "document", "analysis", "summary", "review", "plan", "proposal"};
        String[] docTypes2 = {"Q1", "Q2", "Q3", "Q4", "annual", "monthly", "weekly", "daily"};
        
        Random random = new Random();
        String docType = docTypes[random.nextInt(docTypes.length)];
        String period = docTypes2[random.nextInt(docTypes2.length)];
        
        return String.format("%s_%s_%s_%s_%d.pdf", site, folder, docType, period, index);
    }

    private NodeRef createDummyPdf(NodeRef parentFolder, String fileName) {
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_NAME, fileName);
        properties.put(ContentModel.PROP_TITLE, fileName.replace(".pdf", ""));
        properties.put(ContentModel.PROP_DESCRIPTION, "Dummy PDF file for testing");

        NodeRef nodeRef = nodeService.createNode(
            parentFolder,
            ContentModel.ASSOC_CONTAINS,
            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName),
            ContentModel.TYPE_CONTENT,
            properties
        ).getChildRef();

        ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        writer.setMimetype("application/pdf");
        writer.setEncoding("UTF-8");
        
        String dummyContent = "%PDF-1.4\n1 0 obj\n<<\n/Type /Catalog\n/Pages 2 0 R\n>>\nendobj\n2 0 obj\n<<\n/Type /Pages\n/Kids [3 0 R]\n/Count 1\n>>\nendobj\n3 0 obj\n<<\n/Type /Page\n/Parent 2 0 R\n/MediaBox [0 0 612 792]\n/Contents 4 0 R\n>>\nendobj\n4 0 obj\n<<\n/Length 44\n>>\nstream\nBT\n/F1 12 Tf\n72 720 Td\n(Dummy PDF Content) Tj\nET\nendstream\nendobj\nxref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000204 00000 n \ntrailer\n<<\n/Size 5\n/Root 1 0 R\n>>\nstartxref\n297\n%%EOF";
        writer.putContent(dummyContent);

        return nodeRef;
    }
}
