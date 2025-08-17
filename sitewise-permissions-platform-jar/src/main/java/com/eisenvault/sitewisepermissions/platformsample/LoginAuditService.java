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

import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.Serializable;
import java.util.Map;

/**
 * Service class for retrieving login audit data.
 * This class queries Alfresco's built-in authentication audit logs.
 */
public class LoginAuditService {
    
    private static Log logger = LogFactory.getLog(LoginAuditService.class);
    
    private AuditService auditService;
    
    public void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Get the last login date for a user from audit logs
     * @param username the username to look up
     * @return formatted login date or fallback message
     */
    public String getLastLoginDate(final String username) {
        try {
            if (auditService == null) {
                logger.warn("AuditService is not available");
                return "Audit service not available";
            }
            
            logger.debug("Querying audit data for user: " + username);
            final List<AuditEntry> loginEntries = new ArrayList<AuditEntry>();
            AuditQueryParameters params = new AuditQueryParameters();
            params.setApplicationName("alfresco-access");
            params.setUser(username);
            long fromTime = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L); // 30 days ago
            long toTime = System.currentTimeMillis(); // now
            params.setFromTime(fromTime);
            params.setToTime(toTime);

            auditService.auditQuery(new AuditService.AuditQueryCallback() {
                public boolean valuesRequired() { return true; }
                public boolean handleAuditEntry(Long entryId, String applicationName, String user, long time, Map<String, Serializable> values) {
                    if ("alfresco-access".equals(applicationName) && username.equals(user)) {
                        AuditEntry entry = new AuditEntry();
                        entry.setEntryId(entryId);
                        entry.setApplicationName(applicationName);
                        entry.setUser(user);
                        entry.setTime(time);
                        entry.setValues(values);
                        loginEntries.add(entry);
                    }
                    return true;
                }
                public boolean handleAuditEntryError(Long entryId, String applicationName, Throwable error) {
                    logger.warn("Error in audit entry " + entryId + " for application " + applicationName + ": " + error.getMessage());
                    return true;
                }
            }, params, Integer.MAX_VALUE);

            if (!loginEntries.isEmpty()) {
                Collections.sort(loginEntries, new Comparator<AuditEntry>() {
                    public int compare(AuditEntry e1, AuditEntry e2) {
                        return Long.compare(e2.getTime(), e1.getTime());
                    }
                });
                AuditEntry latestEntry = loginEntries.get(0);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String result = sdf.format(new Date(latestEntry.getTime()));
                logger.debug("Latest login for user " + username + ": " + result);
                return result;
            }
            logger.debug("No login audit data found for user: " + username);
            return "No login audit data found";
        } catch (Exception e) {
            logger.error("Error getting last login date for " + username + ": " + e.getMessage(), e);
            return "Error retrieving login data";
        }
    }
    
    /**
     * Inner class to hold audit entry data
     */
    private static class AuditEntry {
        private Long entryId;
        private String applicationName;
        private String user;
        private long time;
        private Map<String, Serializable> values;
        
        public Long getEntryId() { return entryId; }
        public void setEntryId(Long entryId) { this.entryId = entryId; }
        
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public long getTime() { return time; }
        public void setTime(long time) { this.time = time; }
        
        public Map<String, Serializable> getValues() { return values; }
        public void setValues(Map<String, Serializable> values) { this.values = values; }
    }
}
