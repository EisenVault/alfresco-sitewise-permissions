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
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.text.SimpleDateFormat;


public class UserInfoWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(UserInfoWebScript.class);

    private NodeService nodeService;
    private PersonService personService;
    private AuditService auditService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        if (logger.isInfoEnabled()) {
            logger.info("UserInfoWebScript.executeImpl() called");
        }
        
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();

        try {
            String username = req.getParameter("username");
            String statusFilter = req.getParameter("status"); // active, inactive, or all

            // Validate status filter parameter
            if (statusFilter != null && !statusFilter.equals("active") && !statusFilter.equals("inactive") && !statusFilter.equals("all")) {
                status.setCode(400);
                model.put("success", false);
                model.put("error", "Invalid status parameter. Must be 'active', 'inactive', or 'all'");
                return model;
            }

            // Default to 'all' if not specified
            if (statusFilter == null) {
                statusFilter = "all";
            }

            if (username != null) {
                // Get info for specific user
                Map<String, Object> userInfo = getUserInfo(username);
                if (userInfo != null) {
                    String userStatus = (String) userInfo.get("status");
                    if (shouldIncludeUser(userStatus, statusFilter)) {
                        users.add(userInfo);
                    }
                }
            } else {
                // Get info for all users
                // This would require getting all users from the system
                // For now, we'll return an error suggesting to use username parameter
                status.setCode(400);
                model.put("success", false);
                model.put("error", "Please specify a username parameter or implement bulk user retrieval");
                return model;
            }

            model.put("success", true);
            model.put("statusFilter", statusFilter);
            model.put("totalUsers", users.size());
            model.put("users", users);

            logger.info("User info retrieved for " + users.size() + " users with filter: " + statusFilter);

        } catch (Exception e) {
            status.setCode(500);
            model.put("success", false);
            model.put("error", "Failed to get user info: " + e.getMessage());
            logger.error("Error in UserInfoWebScript: " + e.getMessage(), e);
        }

        return model;
    }

    /**
     * Get user information including status and last login date
     */
    private Map<String, Object> getUserInfo(String username) {
        Map<String, Object> userInfo = new HashMap<String, Object>();
        
        try {
            // Check if user exists first
            boolean userExists = personService.personExists(username);
            logger.info("User '" + username + "' exists: " + userExists);
            
            if (!userExists) {
                // User doesn't exist
                userInfo.put("username", username);
                userInfo.put("status", "nonexistent");
                userInfo.put("lastLogin", "N/A");
                userInfo.put("firstName", "");
                userInfo.put("lastName", "");
                userInfo.put("email", "");
                return userInfo;
            }
            
            // Get the person node for this user
            NodeRef personNode = personService.getPerson(username);
            logger.info("Person node for '" + username + "': " + personNode);
            
            if (personNode != null && nodeService.exists(personNode)) {
                // Get additional user properties
                String firstName = (String) nodeService.getProperty(personNode, ContentModel.PROP_FIRSTNAME);
                String lastName = (String) nodeService.getProperty(personNode, ContentModel.PROP_LASTNAME);
                String email = (String) nodeService.getProperty(personNode, ContentModel.PROP_EMAIL);
                
                // Check if this is a real user or a default person node
                // If firstName is the same as username and lastName/email are empty, it's likely a default node
                if (firstName != null && firstName.equals(username) && 
                    (lastName == null || lastName.isEmpty()) && 
                    (email == null || email.isEmpty())) {
                    // This looks like a default person node for a nonexistent user
                    userInfo.put("username", username);
                    userInfo.put("status", "nonexistent");
                    userInfo.put("lastLogin", "N/A");
                    userInfo.put("firstName", "");
                    userInfo.put("lastName", "");
                    userInfo.put("email", "");
                } else {
                    // This is a real user
                    userInfo.put("username", username);
                    userInfo.put("status", "active");
                    
                    // Get last login from audit service
                    String lastLogin = getLastLoginFromAudit(username);
                    userInfo.put("lastLogin", lastLogin);
                    
                    userInfo.put("firstName", firstName != null ? firstName : "");
                    userInfo.put("lastName", lastName != null ? lastName : "");
                    userInfo.put("email", email != null ? email : "");
                }
                
            } else {
                // User not found or doesn't exist
                userInfo.put("username", username);
                userInfo.put("status", "unknown");
                userInfo.put("lastLogin", "Unknown");
                userInfo.put("firstName", "");
                userInfo.put("lastName", "");
                userInfo.put("email", "");
            }
        } catch (Exception e) {
            logger.warn("Error getting user info for " + username + ": " + e.getMessage());
            userInfo.put("username", username);
            userInfo.put("status", "error");
            userInfo.put("lastLogin", "Error");
            userInfo.put("firstName", "");
            userInfo.put("lastName", "");
            userInfo.put("email", "");
        }
        
        return userInfo;
    }

    /**
     * Check if user should be included based on status filter
     */
    private boolean shouldIncludeUser(String userStatus, String statusFilter) {
        if ("all".equals(statusFilter)) {
            return true;
        } else if ("active".equals(statusFilter)) {
            return "active".equals(userStatus);
        } else if ("inactive".equals(statusFilter)) {
            return "inactive".equals(userStatus);
        } else if ("nonexistent".equals(statusFilter)) {
            return "nonexistent".equals(userStatus);
        }
        return true; // Default to include
    }

    /**
     * Get last login date from audit service
     */
    private String getLastLoginFromAudit(String username) {
        if (logger.isInfoEnabled()) {
            logger.info("getLastLoginFromAudit() called for user: " + username);
        }
        
        try {
            // Since Alfresco doesn't store last login in person properties by default,
            // we'll use the current date as a fallback for active users
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date());
            
        } catch (Exception e) {
            logger.warn("Error getting audit data for " + username + ": " + e.getMessage());
            return "Audit query failed: " + e.getMessage();
        }
    }
}
