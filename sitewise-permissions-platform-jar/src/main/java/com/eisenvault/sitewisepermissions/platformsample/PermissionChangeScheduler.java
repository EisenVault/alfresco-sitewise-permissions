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
import java.util.List;
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
     * Comprehensive method to check for permission changes across all nodes
     * This is more reliable than OnUpdateNodePolicy for permission changes
     */
    public void checkPermissionChanges() {
        try {
            logger.info("=== PERMISSION CHANGE CHECKER STARTED ===");
            
            // Clear the processed nodes cache to ensure we check all nodes
            processedNodes.clear();
            
            Date now = new Date();
            int totalNodesChecked = 0;
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
                        totalNodesChecked++;
                    }
                    
                    // Check document library
                    if (documentLibrary != null && nodeService.exists(documentLibrary)) {
                        int docLibPermissions = checkNodePermissionsInternal(documentLibrary, now);
                        newPermissionsFound += docLibPermissions;
                        totalNodesChecked++;
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
            
            // Also check Company Home and other root-level nodes
            try {
                // Try to get Company Home using the repository service
                NodeRef companyHome = new NodeRef("workspace://SpacesStore/company-home");
                logger.info("Attempting to check Company Home at: " + companyHome);
                if (nodeService.exists(companyHome)) {
                    logger.info("Checking Company Home and all child nodes...");
                    int companyHomePermissions = checkNodePermissionsInternal(companyHome, now);
                    newPermissionsFound += companyHomePermissions;
                    totalNodesChecked++;
                    
                    // Check all child nodes recursively
                    int childPermissions = checkChildNodes(companyHome, now);
                    newPermissionsFound += childPermissions;
                } else {
                    logger.warn("Company Home node not found at expected location: " + companyHome);
                }
            } catch (Exception e) {
                logger.error("Error checking Company Home: " + e.getMessage(), e);
            }
            
            logger.info("=== PERMISSION CHANGE CHECKER COMPLETED ===");
            logger.info("Total nodes checked: " + totalNodesChecked);
            logger.info("New permissions found: " + newPermissionsFound);
            
        } catch (Exception e) {
            logger.error("Error in permission change checker: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check permissions for a specific node (internal method)
     */
    private int checkNodePermissionsInternal(NodeRef nodeRef, Date now) {
        int newPermissionsFound = 0;
        
        try {
            // Skip if we've already processed this node recently
            if (processedNodes.contains(nodeRef)) {
                return 0;
            }
            
            // Add to processed set
            processedNodes.add(nodeRef);
            
            // Check if this node has any permissions set
            Set<AccessPermission> currentPerms = permissionService.getAllSetPermissions(nodeRef);
            
            for (AccessPermission currentPerm : currentPerms) {
                // Skip system permissions
                if (PermissionUtils.isSystemPermission(currentPerm.getAuthority(), currentPerm.getPermission())) {
                    continue;
                }
                
                // Check if this permission was already recorded in the database
                PermissionAuditService.PermissionAuditEntry existingEntry = permissionAuditService.getLatestPermissionGrant(
                    nodeRef, currentPerm.getAuthority(), currentPerm.getPermission());
                
                if (existingEntry == null) {
                    // This is a new permission - record it
                    // Since we can't determine who actually granted this permission,
                    // we'll use "Unknown" as the granted by user
                    String grantedBy = "Unknown";
                    
                    logger.info("[PERMISSION ADDED] Node: " + nodeRef +
                               " | Authority: " + currentPerm.getAuthority() +
                               " | Permission: " + currentPerm.getPermission() +
                               " | By: " + grantedBy);
                    
                    // Record in audit table
                    permissionAuditService.recordPermissionGrant(nodeRef, currentPerm.getAuthority(), 
                        currentPerm.getPermission(), grantedBy, now, null);
                    
                    newPermissionsFound++;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error checking permissions for node " + nodeRef + ": " + e.getMessage(), e);
        }
        
        return newPermissionsFound;
    }
    
    /**
     * Recursively check child nodes
     */
    private int checkChildNodes(NodeRef parentNode, Date now) {
        int totalNewPermissions = 0;
        
        try {
            List<org.alfresco.service.cmr.repository.ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentNode);
            
            for (org.alfresco.service.cmr.repository.ChildAssociationRef assoc : childAssocs) {
                NodeRef childNode = assoc.getChildRef();
                
                if (nodeService.exists(childNode)) {
                    // Check permissions for this child node
                    int childPermissions = checkNodePermissionsInternal(childNode, now);
                    totalNewPermissions += childPermissions;
                    
                    // Recursively check children if this is a folder
                    if (nodeService.getType(childNode).equals(org.alfresco.model.ContentModel.TYPE_FOLDER)) {
                        int grandChildPermissions = checkChildNodes(childNode, now);
                        totalNewPermissions += grandChildPermissions;
                    }
                }
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
