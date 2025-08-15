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
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class AssignUsersToGroupsWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(AssignUsersToGroupsWebScript.class);

    private AuthorityService authorityService;
    private PersonService personService;

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        int assignedCount = 0;
        int errorCount = 0;

        try {
            // Define user-group assignments
            String[][] assignments = {
                // CRM Users
                {"john.doe", "CRM_Managers"},
                {"jane.smith", "CRM_Users"},
                {"mike.johnson", "CRM_Users"},
                {"sarah.williams", "CRM_Admins"},
                {"david.brown", "CRM_Viewers"},
                {"lisa.davis", "CRM_Contributors"},
                {"james.miller", "CRM_Leads_Team"},
                {"emily.wilson", "CRM_Customers_Team"},
                {"robert.moore", "CRM_Sales_Team"},
                {"jennifer.taylor", "CRM_Users"},
                
                // HR Users
                {"alice.jones", "HR_Managers"},
                {"bob.wilson", "HR_Users"},
                {"carol.martinez", "HR_Admins"},
                {"david.anderson", "HR_Viewers"},
                {"elizabeth.taylor", "HR_Contributors"},
                {"frank.thomas", "HR_Recruitment_Team"},
                {"grace.hernandez", "HR_Employee_Relations"},
                {"henry.moore", "HR_Payroll_Team"},
                {"irene.martin", "HR_Training_Team"},
                {"jack.lee", "HR_Users"},
                
                // Finance Users
                {"alex.turner", "Finance_Managers"},
                {"bella.campbell", "Finance_Users"},
                {"chris.parker", "Finance_Admins"},
                {"daisy.evans", "Finance_Viewers"},
                {"evan.edwards", "Finance_Contributors"},
                {"faye.collins", "Finance_Accounting_Team"},
                {"gavin.stewart", "Finance_Budget_Team"},
                {"hannah.sanchez", "Finance_Audit_Team"},
                {"ivan.morris", "Finance_Tax_Team"},
                {"jade.rogers", "Finance_Users"},
                
                // Cross-functional assignments
                {"john.doe", "Executive_Team"},
                {"jane.smith", "Department_Heads"},
                {"alice.jones", "Department_Heads"},
                {"alex.turner", "Department_Heads"},
                {"mike.johnson", "Project_Managers"},
                {"sarah.williams", "IT_Support"},
                {"david.brown", "Legal_Team"},
                {"lisa.davis", "Compliance_Team"}
            };

            for (String[] assignment : assignments) {
                String userName = assignment[0];
                String groupName = assignment[1];

                try {
                    // Check if user exists
                    if (!personService.personExists(userName)) {
                        logger.warn("User does not exist: " + userName);
                        continue;
                    }

                    // Check if group exists
                    if (!authorityService.authorityExists(groupName)) {
                        logger.warn("Group does not exist: " + groupName);
                        continue;
                    }

                    // Check if user is already a member of the group
                    if (authorityService.authorityExists(userName)) {
                        Set<String> userAuthorities = authorityService.getAuthoritiesForUser(userName);
                        if (userAuthorities.contains(groupName)) {
                            logger.info("User " + userName + " is already a member of " + groupName);
                            
                            Map<String, String> result = new HashMap<String, String>();
                            result.put("user", userName);
                            result.put("group", groupName);
                            result.put("status", "exists");
                            result.put("message", "User already a member of group");
                            results.add(result);
                            assignedCount++;
                            continue;
                        }
                    }

                    // Add user to group
                    authorityService.addAuthority(groupName, userName);
                    
                    logger.info("Added user " + userName + " to group " + groupName);
                    
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("user", userName);
                    result.put("group", groupName);
                    result.put("status", "assigned");
                    result.put("message", "User successfully added to group");
                    results.add(result);
                    assignedCount++;

                } catch (Exception e) {
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("user", userName);
                    result.put("group", groupName);
                    result.put("status", "error");
                    result.put("message", "Error assigning user to group: " + e.getMessage());
                    results.add(result);
                    errorCount++;
                    
                    logger.error("Error assigning user " + userName + " to group " + groupName + ": " + e.getMessage(), e);
                }
            }

            model.put("message", "User-group assignments completed");
            model.put("totalAssignments", assignments.length);
            model.put("assignedCount", assignedCount);
            model.put("errorCount", errorCount);
            model.put("results", results);

        } catch (Exception e) {
            status.setCode(500);
            model.put("error", "Failed to assign users to groups: " + e.getMessage());
            logger.error("Error assigning users to groups: " + e.getMessage(), e);
        }

        return model;
    }
}
