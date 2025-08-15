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
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class AddUsersToSitesWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(AddUsersToSitesWebScript.class);

    private SiteService siteService;
    private PersonService personService;

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        int addedCount = 0;
        int errorCount = 0;

        try {
            // Define user-site assignments
            String[][] assignments = {
                // CRM Site Members
                {"john.doe", "crm", "SiteManager"},
                {"jane.smith", "crm", "SiteContributor"},
                {"mike.johnson", "crm", "SiteContributor"},
                {"sarah.williams", "crm", "SiteManager"},
                {"david.brown", "crm", "SiteConsumer"},
                
                // HR Site Members
                {"alice.jones", "hr", "SiteManager"},
                {"bob.wilson", "hr", "SiteContributor"},
                {"carol.martinez", "hr", "SiteManager"},
                {"david.anderson", "hr", "SiteConsumer"},
                {"elizabeth.taylor", "hr", "SiteContributor"},
                
                // Finance Site Members
                {"alex.turner", "finance", "SiteManager"},
                {"bella.campbell", "finance", "SiteContributor"},
                {"chris.parker", "finance", "SiteManager"},
                {"daisy.evans", "finance", "SiteConsumer"},
                {"evan.edwards", "finance", "SiteContributor"}
            };

            for (String[] assignment : assignments) {
                String userName = assignment[0];
                String siteShortName = assignment[1];
                String role = assignment[2];

                try {
                    // Check if user exists
                    if (!personService.personExists(userName)) {
                        logger.warn("User does not exist: " + userName);
                        continue;
                    }

                    // Check if site exists
                    SiteInfo siteInfo = siteService.getSite(siteShortName);
                    if (siteInfo == null) {
                        logger.warn("Site does not exist: " + siteShortName);
                        continue;
                    }

                    // Check if user is already a member of the site
                    if (siteService.isMember(siteShortName, userName)) {
                        logger.info("User " + userName + " is already a member of site " + siteShortName);
                        
                        Map<String, String> result = new HashMap<String, String>();
                        result.put("user", userName);
                        result.put("site", siteShortName);
                        result.put("status", "exists");
                        result.put("message", "User already a member of site");
                        results.add(result);
                        addedCount++;
                        continue;
                    }

                    // Add user to site
                    siteService.setMembership(siteShortName, userName, role);
                    
                    logger.info("Added user " + userName + " to site " + siteShortName + " with role " + role);
                    
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("user", userName);
                    result.put("site", siteShortName);
                    result.put("role", role);
                    result.put("status", "added");
                    result.put("message", "User successfully added to site");
                    results.add(result);
                    addedCount++;

                } catch (Exception e) {
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("user", userName);
                    result.put("site", siteShortName);
                    result.put("status", "error");
                    result.put("message", "Error adding user to site: " + e.getMessage());
                    results.add(result);
                    errorCount++;
                    
                    logger.error("Error adding user " + userName + " to site " + siteShortName + ": " + e.getMessage(), e);
                }
            }

            model.put("message", "User-site assignments completed");
            model.put("totalAssignments", assignments.length);
            model.put("addedCount", addedCount);
            model.put("errorCount", errorCount);
            model.put("results", results);

        } catch (Exception e) {
            status.setCode(500);
            model.put("error", "Failed to add users to sites: " + e.getMessage());
            logger.error("Error adding users to sites: " + e.getMessage(), e);
        }

        return model;
    }
}
