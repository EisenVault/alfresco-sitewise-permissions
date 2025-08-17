package com.eisenvault.sitewisepermissions.platformsample;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive web script to manually trigger permission checking for testing purposes.
 * This allows testing the permission audit functionality with various options:
 * - Check all sites
 * - Check specific site
 * - Check specific node
 * - Clear cache
 */
public class PermissionCheckerWebScript extends AbstractWebScript {
    
    private static Log logger = LogFactory.getLog(PermissionCheckerWebScript.class);
    
    private PermissionChangeScheduler permissionChangeScheduler;
    private SiteService siteService;
    private NodeService nodeService;
    
    public void setPermissionChangeScheduler(PermissionChangeScheduler permissionChangeScheduler) {
        this.permissionChangeScheduler = permissionChangeScheduler;
    }
    
    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }
    
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    
    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        try {
            String siteShortName = req.getParameter("site");
            String nodeRefStr = req.getParameter("nodeRef");
            String action = req.getParameter("action");
            
            Map<String, Object> result = new HashMap<String, Object>();
            
            if ("clear-cache".equals(action)) {
                // Clear the processed nodes cache
                permissionChangeScheduler.clearProcessedNodesCache();
                result.put("status", "success");
                result.put("message", "Permission cache cleared successfully");
                result.put("action", "clear-cache");
                
            } else if ("check-permissions".equals(action) || action == null) {
                // Trigger comprehensive permission checking
                permissionChangeScheduler.checkPermissionChanges();
                result.put("status", "success");
                result.put("message", "Permission checking completed successfully");
                result.put("action", "check-permissions");
                
            } else if (nodeRefStr != null) {
                // Check permissions for a specific node
                NodeRef nodeRef = new NodeRef(nodeRefStr);
                if (nodeService.exists(nodeRef)) {
                    permissionChangeScheduler.checkNodePermissions(nodeRef);
                    result.put("status", "success");
                    result.put("message", "Permission checking completed for node: " + nodeRef);
                    result.put("nodeRef", nodeRef.toString());
                    result.put("action", "check-node");
                } else {
                    res.setStatus(404);
                    result.put("status", "error");
                    result.put("message", "Node not found: " + nodeRefStr);
                }
                
            } else if (siteShortName != null) {
                // Check permissions for all nodes in a specific site
                SiteInfo siteInfo = siteService.getSite(siteShortName);
                if (siteInfo == null) {
                    res.setStatus(404);
                    result.put("status", "error");
                    result.put("message", "Site not found: " + siteShortName);
                } else {
                    NodeRef siteNodeRef = siteInfo.getNodeRef();
                    NodeRef documentLibrary = siteService.getContainer(siteShortName, "documentLibrary");
                    
                    List<String> processedNodes = new ArrayList<String>();
                    
                    // Check permissions for site node
                    if (nodeService.exists(siteNodeRef)) {
                        permissionChangeScheduler.checkNodePermissions(siteNodeRef);
                        processedNodes.add(siteNodeRef.toString());
                    }
                    
                    // Check permissions for document library
                    if (documentLibrary != null && nodeService.exists(documentLibrary)) {
                        permissionChangeScheduler.checkNodePermissions(documentLibrary);
                        processedNodes.add(documentLibrary.toString());
                    }
                    
                    result.put("status", "success");
                    result.put("message", "Permission checking completed for site: " + siteShortName);
                    result.put("site", siteShortName);
                    result.put("processedNodes", processedNodes);
                    result.put("action", "check-site");
                }
                
            } else {
                // No parameters provided - show usage
                res.setStatus(400);
                result.put("status", "error");
                result.put("message", "Missing required parameters");
                result.put("usage", "Use one of: ?action=check-permissions | ?action=clear-cache | ?site=<siteName> | ?nodeRef=<nodeRef>");
            }
            
            // Write JSON response
            res.setContentType("application/json");
            res.getWriter().write("{\"status\": \"" + result.get("status") + "\", \"message\": \"" + result.get("message") + "\"}");
            
        } catch (Exception e) {
            logger.error("Error in permission checker web script: " + e.getMessage(), e);
            res.setStatus(500);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\": \"error\", \"message\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }
}
