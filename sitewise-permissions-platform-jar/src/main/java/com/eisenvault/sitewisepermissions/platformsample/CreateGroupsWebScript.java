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
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CreateGroupsWebScript extends DeclarativeWebScript {
    private static Log logger = LogFactory.getLog(CreateGroupsWebScript.class);

    private AuthorityService authorityService;

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        
        // Define test groups (comprehensive groups for each site)
        String[][] groups = {
            // CRM Groups
            {"CRM_Managers", "CRM Managers"},
            {"CRM_Users", "CRM Users"},
            {"CRM_Admins", "CRM Administrators"},
            {"CRM_Viewers", "CRM Viewers"},
            {"CRM_Contributors", "CRM Contributors"},
            {"CRM_Leads_Team", "CRM Leads Team"},
            {"CRM_Customers_Team", "CRM Customers Team"},
            {"CRM_Sales_Team", "CRM Sales Team"},
            
            // HR Groups
            {"HR_Managers", "HR Managers"},
            {"HR_Users", "HR Users"},
            {"HR_Admins", "HR Administrators"},
            {"HR_Viewers", "HR Viewers"},
            {"HR_Contributors", "HR Contributors"},
            {"HR_Recruitment_Team", "HR Recruitment Team"},
            {"HR_Employee_Relations", "HR Employee Relations"},
            {"HR_Payroll_Team", "HR Payroll Team"},
            {"HR_Training_Team", "HR Training Team"},
            
            // Finance Groups
            {"Finance_Managers", "Finance Managers"},
            {"Finance_Users", "Finance Users"},
            {"Finance_Admins", "Finance Administrators"},
            {"Finance_Viewers", "Finance Viewers"},
            {"Finance_Contributors", "Finance Contributors"},
            {"Finance_Accounting_Team", "Finance Accounting Team"},
            {"Finance_Budget_Team", "Finance Budget Team"},
            {"Finance_Audit_Team", "Finance Audit Team"},
            {"Finance_Tax_Team", "Finance Tax Team"},
            
            // Cross-functional Groups
            {"Executive_Team", "Executive Team"},
            {"Department_Heads", "Department Heads"},
            {"Project_Managers", "Project Managers"},
            {"IT_Support", "IT Support"},
            {"Legal_Team", "Legal Team"},
            {"Compliance_Team", "Compliance Team"}
        };

        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        int createdCount = 0;
        int existingCount = 0;

        for (String[] group : groups) {
            String shortName = group[0];
            String displayName = group[1];
            
            try {
                if (!authorityService.authorityExists(shortName)) {
                    authorityService.createAuthority(AuthorityType.GROUP, shortName, displayName, authorityService.getDefaultZones());
                    
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("groupName", shortName);
                    result.put("status", "created");
                    result.put("message", "Group created successfully");
                    results.add(result);
                    createdCount++;
                    
                    logger.info("Created group: " + shortName);
                } else {
                    Map<String, String> result = new HashMap<String, String>();
                    result.put("groupName", shortName);
                    result.put("status", "exists");
                    result.put("message", "Group already exists");
                    results.add(result);
                    existingCount++;
                    
                    logger.info("Group already exists: " + shortName);
                }
            } catch (org.alfresco.service.cmr.repository.DuplicateChildNodeNameException e) {
                // Group already exists but authorityExists() didn't catch it
                Map<String, String> result = new HashMap<String, String>();
                result.put("groupName", shortName);
                result.put("status", "exists");
                result.put("message", "Group already exists (detected via exception)");
                results.add(result);
                existingCount++;
                
                logger.info("Group already exists (detected via exception): " + shortName);
            } catch (Exception e) {
                Map<String, String> result = new HashMap<String, String>();
                result.put("groupName", shortName);
                result.put("status", "error");
                result.put("message", "Error creating group: " + e.getMessage());
                results.add(result);
                
                logger.error("Error creating group " + shortName + ": " + e.getMessage(), e);
            }
        }

        model.put("results", results);
        model.put("createdCount", createdCount);
        model.put("existingCount", existingCount);
        model.put("totalGroups", groups.length);

        return model;
    }
}
