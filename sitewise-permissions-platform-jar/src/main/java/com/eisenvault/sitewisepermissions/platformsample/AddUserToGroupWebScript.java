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
import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class AddUserToGroupWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(AddUserToGroupWebScript.class);

    private AuthorityService authorityService;

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        
        String groupName = req.getParameter("group");
        String userName = req.getParameter("user");
        
        if (groupName == null || userName == null) {
            status.setCode(400);
            model.put("error", "Missing required parameters: group and user");
            return model;
        }
        
        try {
            // Check if group exists - try different formats
            boolean groupExists = authorityService.authorityExists(groupName);
            if (!groupExists) {
                // Try with GROUP_ prefix
                groupExists = authorityService.authorityExists("GROUP_" + groupName);
                if (groupExists) {
                    groupName = "GROUP_" + groupName;
                }
            }
            
            if (!groupExists) {
                status.setCode(404);
                model.put("error", "Group " + groupName + " does not exist");
                logger.error("Group not found: " + groupName);
                return model;
            }
            
            // Add user to group
            authorityService.addAuthority(groupName, userName);
            
            model.put("success", true);
            model.put("message", "User " + userName + " added to group " + groupName);
            logger.info("Added user " + userName + " to group " + groupName);
            
        } catch (Exception e) {
            status.setCode(500);
            model.put("error", "Failed to add user to group: " + e.getMessage());
            logger.error("Error adding user " + userName + " to group " + groupName + ": " + e.getMessage(), e);
        }

        return model;
    }
}
