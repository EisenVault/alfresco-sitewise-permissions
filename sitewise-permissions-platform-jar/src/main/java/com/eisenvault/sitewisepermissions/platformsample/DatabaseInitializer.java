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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database initializer for the permission audit table.
 * This class automatically creates the required database table when the module is deployed.
 */
public class DatabaseInitializer {
    
    private static Log logger = LogFactory.getLog(DatabaseInitializer.class);
    
    private JdbcTemplate jdbcTemplate;
    
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Initialize the database by creating the permission_audit table
     */
    @Transactional
    public void init() {
        try {
            logger.info("Initializing permission audit database table...");
            
            // Create the permission_audit table (H2 compatible - no inline indexes)
            String createTableSql = 
                "CREATE TABLE IF NOT EXISTS permission_audit (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "node_ref VARCHAR(255) NOT NULL, " +
                "user_granted_to VARCHAR(255) NOT NULL, " +
                "granted_by VARCHAR(255) NOT NULL, " +
                "date_granted TIMESTAMP NOT NULL, " +
                "expiry_date TIMESTAMP NULL, " +
                "permission VARCHAR(255) NOT NULL, " +
                "action_type VARCHAR(50) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            jdbcTemplate.execute(createTableSql);
            
            // Create indexes separately (H2 compatible)
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_node_ref ON permission_audit (node_ref)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_granted_to ON permission_audit (user_granted_to)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_granted_by ON permission_audit (granted_by)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_date_granted ON permission_audit (date_granted)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_action_type ON permission_audit (action_type)");
            } catch (Exception e) {
                // Index creation might fail if they already exist, log but don't fail
                logger.debug("Could not create some indexes (they may already exist): " + e.getMessage());
            }
            
            // Add table comment
            try {
                jdbcTemplate.execute("ALTER TABLE permission_audit COMMENT = 'Stores permission grant/revoke events for audit tracking'");
            } catch (Exception e) {
                // Comment might not be supported in all databases, ignore error
                logger.debug("Could not add table comment: " + e.getMessage());
            }
            
            // Insert initialization record (H2 compatible - no INSERT IGNORE)
            try {
                // Check if initialization record already exists
                String checkSql = "SELECT COUNT(*) FROM permission_audit WHERE node_ref = 'system://init'";
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
                
                if (count == null || count == 0) {
                    // Only insert if record doesn't exist
                    String initRecordSql = 
                        "INSERT INTO permission_audit (node_ref, user_granted_to, granted_by, date_granted, permission, action_type) " +
                        "VALUES ('system://init', 'system', 'system', NOW(), 'INIT', 'GRANT')";
                    
                    jdbcTemplate.execute(initRecordSql);
                    logger.info("Initialization record inserted successfully");
                } else {
                    logger.info("Initialization record already exists, skipping insert");
                }
            } catch (Exception e) {
                // Log but don't fail initialization
                logger.warn("Could not insert initialization record: " + e.getMessage());
            }
            
            logger.info("Permission audit database table initialized successfully");
            
        } catch (Exception e) {
            logger.error("Error initializing permission audit database table: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize permission audit database", e);
        }
    }
    
    /**
     * Check if the permission_audit table exists
     * @return true if the table exists, false otherwise
     */
    public boolean isTableExists() {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM permission_audit WHERE node_ref = 'system://init'";
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.debug("Permission audit table does not exist: " + e.getMessage());
            return false;
        }
    }
}
