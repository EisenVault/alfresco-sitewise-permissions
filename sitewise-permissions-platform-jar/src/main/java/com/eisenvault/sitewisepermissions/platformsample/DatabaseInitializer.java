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
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database initializer for the permission audit table.
 * This class automatically creates the required database table when the module is deployed.
 * Supports multiple database types: H2, MySQL, PostgreSQL, SQL Server.
 */
public class DatabaseInitializer {
    
    private static Log logger = LogFactory.getLog(DatabaseInitializer.class);
    
    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;
    private String databaseType;
    
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Initialize the database by creating the permission_audit table
     */
    public void init() {
        try {
            logger.info("Initializing permission audit database table...");
            
            // Validate dependencies
            if (jdbcTemplate == null) {
                throw new RuntimeException("JdbcTemplate is not available");
            }
            
            // Detect database type
            detectDatabaseType();
            
            // Check if table exists
            boolean tableExists = isTableExists();
            logger.info("Table exists check result: " + tableExists);
            
            if (tableExists) {
                // Migrate existing table to new schema
                logger.info("Table exists, checking for schema migration...");
                migrateTableSchema();
            } else {
                // Create new table with current schema
                logger.info("Table does not exist, creating new table...");
                createTable();
                
                // Verify table creation by checking if it's accessible
                logger.debug("Verifying table creation with a test query...");
                int retryCount = 0;
                boolean tableAccessible = false;
                
                while (!tableAccessible && retryCount < 5) {
                    try {
                        jdbcTemplate.execute("SELECT 1 FROM permission_audit LIMIT 1");
                        tableAccessible = true;
                        logger.debug("Table creation verified successfully on attempt " + (retryCount + 1));
                    } catch (Exception e) {
                        retryCount++;
                        logger.debug("Table not yet accessible, attempt " + retryCount + " of 5: " + e.getMessage());
                        if (retryCount < 5) {
                            try {
                                Thread.sleep(200 * retryCount); // Exponential backoff
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                
                if (!tableAccessible) {
                    logger.warn("Table creation verification failed after 5 attempts, proceeding anyway");
                } else {
                    logger.info("Table creation verified successfully");
                }
            }
            
            // Create indexes
            createIndexes();
            
            // Verify table is accessible before inserting initialization record
            logger.debug("Checking if table is accessible for initialization record insertion...");
            if (isTableAccessible()) {
                logger.debug("Table is accessible, proceeding with initialization record insertion");
                // Insert initialization record
                insertInitializationRecord();
            } else {
                logger.warn("Table is not accessible, skipping initialization record insertion");
            }
            
            logger.info("Permission audit database table initialization process completed");
            
        } catch (Exception e) {
            logger.error("Error initializing permission audit database table: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize permission audit database", e);
        }
    }
    
    /**
     * Detect the database type from the connection
     */
    private void detectDatabaseType() throws SQLException {
        if (dataSource == null) {
            logger.warn("DataSource not available, assuming H2 database");
            databaseType = "H2";
            return;
        }
        
        try (java.sql.Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            String productVersion = metaData.getDatabaseProductVersion();
            
            logger.debug("Database product: " + productName + " version: " + productVersion);
            
            if (productName.contains("h2")) {
                databaseType = "H2";
            } else if (productName.contains("mysql")) {
                databaseType = "MySQL";
            } else if (productName.contains("postgresql")) {
                databaseType = "PostgreSQL";
            } else if (productName.contains("microsoft sql server")) {
                databaseType = "SQLServer";
            } else {
                logger.warn("Unknown database type: " + productName + ", assuming H2");
                databaseType = "H2";
            }
            
            logger.info("Detected database type: " + databaseType);
        } catch (Exception e) {
            logger.warn("Error detecting database type: " + e.getMessage() + ", assuming H2");
            databaseType = "H2";
        }
        
        // Log additional database connection information for debugging
        try (java.sql.Connection connection = dataSource.getConnection()) {
            logger.debug("Database connection auto-commit: " + connection.getAutoCommit());
            logger.debug("Database connection transaction isolation: " + connection.getTransactionIsolation());
        } catch (Exception e) {
            logger.debug("Could not get connection details: " + e.getMessage());
        }
    }
    
    /**
     * Check if the permission_audit table is accessible for queries
     * @return true if the table is accessible, false otherwise
     */
    private boolean isTableAccessible() {
        try {
            jdbcTemplate.execute("SELECT 1 FROM permission_audit LIMIT 1");
            return true;
        } catch (Exception e) {
            logger.debug("Table accessibility check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the permission_audit table exists using database metadata
     * @return true if the table exists, false otherwise
     */
    public boolean isTableExists() {
        try {
            if (dataSource == null) {
                // Fallback to query method if DataSource not available
                logger.debug("DataSource not available, using query method for table existence check");
                return isTableExistsByQuery();
            }
            
            try (java.sql.Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String catalog = connection.getCatalog();
                logger.debug("Checking table existence in catalog: " + catalog);
                
                // For Alfresco 5.2 compatibility, don't use getSchema() method
                // Try different schema patterns without using getSchema()
                String[] schemas = {null, "PUBLIC", "dbo", "ALFRESCO"};
                
                // Try different table name patterns for case sensitivity
                String[] tableNames = {"permission_audit", "PERMISSION_AUDIT", "Permission_Audit"};
                
                for (String schemaName : schemas) {
                    for (String tableName : tableNames) {
                        try (ResultSet tables = metaData.getTables(catalog, schemaName, tableName, new String[]{"TABLE"})) {
                            if (tables.next()) {
                                logger.debug("Table found in schema: " + schemaName + " with name: " + tableName);
                                return true;
                            }
                        } catch (SQLException e) {
                            logger.debug("Error checking schema " + schemaName + " table " + tableName + ": " + e.getMessage());
                        }
                    }
                }
                
                logger.debug("Table not found in any schema");
                return false;
            }
        } catch (Exception e) {
            logger.warn("Error checking table existence via metadata, falling back to query method: " + e.getMessage());
            return isTableExistsByQuery();
        }
    }
    
    /**
     * Fallback method to check table existence by querying
     */
    private boolean isTableExistsByQuery() {
        logger.debug("Using query method to check table existence");
        // Try different case variations for the table name
        String[] tableNames = {"permission_audit", "PERMISSION_AUDIT", "Permission_Audit"};
        
        for (String tableName : tableNames) {
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
                logger.debug("Table exists with name: " + tableName);
                return true;
            } catch (Exception e) {
                logger.debug("Table does not exist with name " + tableName + ": " + e.getMessage());
            }
        }
        
        logger.debug("Table not found with any name variation");
        return false;
    }
    
    /**
     * Create the permission_audit table with database-specific syntax
     */
    private void createTable() {
        String createTableSql = getCreateTableSql();
        logger.info("Executing table creation SQL: " + createTableSql);
        
        try {
            jdbcTemplate.execute(createTableSql);
            logger.info("Table created successfully");
        } catch (Exception e) {
            // Check if the error is due to table already existing
            if (e.getMessage().contains("already exists") || e.getMessage().contains("Table") && e.getMessage().contains("exists")) {
                logger.info("Table already exists, skipping creation: " + e.getMessage());
                return;
            }
            // If it's a different error, re-throw it
            logger.error("Error creating table: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get database-specific CREATE TABLE SQL
     */
    private String getCreateTableSql() {
        switch (databaseType) {
            case "MySQL":
                return "CREATE TABLE permission_audit (" +
                       "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                       "node_ref VARCHAR(255) NOT NULL, " +
                       "user_granted_to VARCHAR(255) NOT NULL, " +
                       "date_granted TIMESTAMP NOT NULL, " +
                       "expiry_date TIMESTAMP NULL, " +
                       "permission VARCHAR(255) NOT NULL, " +
                       "action_type VARCHAR(50) NOT NULL, " +
                       "is_active BOOLEAN DEFAULT TRUE, " +
                       "revoked_date TIMESTAMP NULL, " +
                       "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                       ")";
            
            case "PostgreSQL":
                return "CREATE TABLE permission_audit (" +
                       "id BIGSERIAL PRIMARY KEY, " +
                       "node_ref VARCHAR(255) NOT NULL, " +
                       "user_granted_to VARCHAR(255) NOT NULL, " +
                       "date_granted TIMESTAMP NOT NULL, " +
                       "expiry_date TIMESTAMP NULL, " +
                       "permission VARCHAR(255) NOT NULL, " +
                       "action_type VARCHAR(50) NOT NULL, " +
                       "is_active BOOLEAN DEFAULT TRUE, " +
                       "revoked_date TIMESTAMP NULL, " +
                       "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                       ")";
            
            case "SQLServer":
                return "CREATE TABLE permission_audit (" +
                       "id BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                       "node_ref NVARCHAR(255) NOT NULL, " +
                       "user_granted_to NVARCHAR(255) NOT NULL, " +
                       "date_granted DATETIME2 NOT NULL, " +
                       "expiry_date DATETIME2 NULL, " +
                       "permission NVARCHAR(255) NOT NULL, " +
                       "action_type NVARCHAR(50) NOT NULL, " +
                       "is_active BIT DEFAULT 1, " +
                       "revoked_date DATETIME2 NULL, " +
                       "created_at DATETIME2 DEFAULT GETDATE()" +
                       ")";
            
            case "H2":
            default:
                return "CREATE TABLE permission_audit (" +
                       "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                       "node_ref VARCHAR(255) NOT NULL, " +
                       "user_granted_to VARCHAR(255) NOT NULL, " +
                       "date_granted TIMESTAMP NOT NULL, " +
                       "expiry_date TIMESTAMP NULL, " +
                       "permission VARCHAR(255) NOT NULL, " +
                       "action_type VARCHAR(50) NOT NULL, " +
                       "is_active BOOLEAN DEFAULT TRUE, " +
                       "revoked_date TIMESTAMP NULL, " +
                       "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                       ")";
        }
    }
    
    /**
     * Create indexes with database-specific syntax
     */
    private void createIndexes() {
        logger.info("Creating indexes...");
        
        String[] indexSqls = getIndexSqls();
        
        for (String indexSql : indexSqls) {
            try {
                logger.debug("Creating index: " + indexSql);
                jdbcTemplate.execute(indexSql);
            } catch (Exception e) {
                logger.debug("Could not create index (may already exist): " + e.getMessage());
            }
        }
        
        logger.info("Index creation completed");
    }
    
    /**
     * Get database-specific index creation SQL
     */
    private String[] getIndexSqls() {
        switch (databaseType) {
            case "MySQL":
                return new String[]{
                    "CREATE INDEX IF NOT EXISTS idx_node_ref ON permission_audit (node_ref)",
                    "CREATE INDEX IF NOT EXISTS idx_user_granted_to ON permission_audit (user_granted_to)",
                    "CREATE INDEX IF NOT EXISTS idx_date_granted ON permission_audit (date_granted)",
                    "CREATE INDEX IF NOT EXISTS idx_action_type ON permission_audit (action_type)",
                    "CREATE INDEX IF NOT EXISTS idx_is_active ON permission_audit (is_active)"
                };
            
            case "PostgreSQL":
                return new String[]{
                    "CREATE INDEX IF NOT EXISTS idx_node_ref ON permission_audit (node_ref)",
                    "CREATE INDEX IF NOT EXISTS idx_user_granted_to ON permission_audit (user_granted_to)",
                    "CREATE INDEX IF NOT EXISTS idx_date_granted ON permission_audit (date_granted)",
                    "CREATE INDEX IF NOT EXISTS idx_action_type ON permission_audit (action_type)",
                    "CREATE INDEX IF NOT EXISTS idx_is_active ON permission_audit (is_active)"
                };
            
            case "SQLServer":
                return new String[]{
                    "CREATE INDEX idx_node_ref ON permission_audit (node_ref)",
                    "CREATE INDEX idx_user_granted_to ON permission_audit (user_granted_to)",
                    "CREATE INDEX idx_date_granted ON permission_audit (date_granted)",
                    "CREATE INDEX idx_action_type ON permission_audit (action_type)",
                    "CREATE INDEX idx_is_active ON permission_audit (is_active)"
                };
            
            case "H2":
            default:
                return new String[]{
                    "CREATE INDEX IF NOT EXISTS idx_node_ref ON permission_audit (node_ref)",
                    "CREATE INDEX IF NOT EXISTS idx_user_granted_to ON permission_audit (user_granted_to)",
                    "CREATE INDEX IF NOT EXISTS idx_date_granted ON permission_audit (date_granted)",
                    "CREATE INDEX IF NOT EXISTS idx_action_type ON permission_audit (action_type)",
                    "CREATE INDEX IF NOT EXISTS idx_is_active ON permission_audit (is_active)"
                };
        }
    }
    
    /**
     * Insert initialization record
     */
    private void insertInitializationRecord() {
        try {
            logger.debug("Starting initialization record insertion process...");
            
            // Check if initialization record already exists
            String checkSql = "SELECT COUNT(*) FROM permission_audit WHERE node_ref = 'system://init'";
            logger.debug("Checking for existing initialization record with SQL: " + checkSql);
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (count == null || count == 0) {
                // Only insert if record doesn't exist
                String initRecordSql = getInitRecordSql();
                logger.debug("Inserting initialization record: " + initRecordSql);
                jdbcTemplate.execute(initRecordSql);
                logger.info("Initialization record inserted successfully");
            } else {
                logger.info("Initialization record already exists, skipping insert");
            }
        } catch (Exception e) {
            logger.warn("Could not insert initialization record: " + e.getMessage());
            // Log the full exception for debugging
            logger.debug("Full exception details:", e);
            
            // Check if it's a table not found error
            if (e.getMessage().contains("relation") && e.getMessage().contains("does not exist")) {
                logger.warn("Table 'permission_audit' appears to not exist yet. This may be a transaction timing issue.");
            }
        }
    }
    
    /**
     * Get database-specific initialization record SQL
     */
    private String getInitRecordSql() {
        switch (databaseType) {
            case "MySQL":
            case "H2":
                return "INSERT INTO permission_audit (node_ref, user_granted_to, date_granted, permission, action_type, is_active) " +
                       "VALUES ('system://init', 'system', NOW(), 'INIT', 'GRANT', TRUE)";
            
            case "PostgreSQL":
                return "INSERT INTO permission_audit (node_ref, user_granted_to, date_granted, permission, action_type, is_active) " +
                       "VALUES ('system://init', 'system', CURRENT_TIMESTAMP, 'INIT', 'GRANT', TRUE)";
            
            case "SQLServer":
                return "INSERT INTO permission_audit (node_ref, user_granted_to, date_granted, permission, action_type, is_active) " +
                       "VALUES ('system://init', 'system', GETDATE(), 'INIT', 'GRANT', 1)";
            
            default:
                return "INSERT INTO permission_audit (node_ref, user_granted_to, date_granted, permission, action_type, is_active) " +
                       "VALUES ('system://init', 'system', NOW(), 'INIT', 'GRANT', TRUE)";
        }
    }
    
    /**
     * Migrate existing table to new schema
     */
    private void migrateTableSchema() {
        try {
            logger.info("Migrating existing permission_audit table to new schema...");
            
            // Check if we need to add new columns
            boolean needsMigration = checkIfMigrationNeeded();
            
            if (!needsMigration) {
                logger.info("Table already has current schema, no migration needed");
                return;
            }
            
            logger.info("Adding new columns to existing table...");
            
            // Add new columns with database-specific syntax
            addNewColumns();
            
            // Drop the old granted_by column if it exists
            dropOldColumns();
            
            // Update existing records to set is_active = true
            updateExistingRecords();
            
            logger.info("Successfully migrated permission_audit table to new schema");
            
        } catch (Exception e) {
            logger.error("Error migrating table schema: " + e.getMessage(), e);
            throw new RuntimeException("Failed to migrate table schema", e);
        }
    }
    
    /**
     * Check if migration is needed by testing column existence
     */
    private boolean checkIfMigrationNeeded() {
        try {
            jdbcTemplate.queryForObject("SELECT is_active FROM permission_audit LIMIT 1", Boolean.class);
            return false; // Column exists, no migration needed
        } catch (Exception e) {
            return true; // Column doesn't exist, migration needed
        }
    }
    
    /**
     * Add new columns with database-specific syntax
     */
    private void addNewColumns() {
        try {
            switch (databaseType) {
                case "MySQL":
                case "H2":
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD COLUMN revoked_date TIMESTAMP NULL");
                    break;
                
                case "PostgreSQL":
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD COLUMN revoked_date TIMESTAMP NULL");
                    break;
                
                case "SQLServer":
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD is_active BIT DEFAULT 1");
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD revoked_date DATETIME2 NULL");
                    break;
                
                default:
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
                    jdbcTemplate.execute("ALTER TABLE permission_audit ADD COLUMN revoked_date TIMESTAMP NULL");
                    break;
            }
        } catch (Exception e) {
            logger.warn("Could not add some columns (they may already exist): " + e.getMessage());
        }
    }
    
    /**
     * Drop old columns that are no longer needed
     */
    private void dropOldColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE permission_audit DROP COLUMN granted_by");
            logger.info("Dropped old granted_by column");
        } catch (Exception e) {
            logger.debug("granted_by column doesn't exist or couldn't be dropped: " + e.getMessage());
        }
    }
    
    /**
     * Update existing records to set is_active = true
     */
    private void updateExistingRecords() {
        try {
            switch (databaseType) {
                case "SQLServer":
                    jdbcTemplate.execute("UPDATE permission_audit SET is_active = 1 WHERE is_active IS NULL");
                    break;
                default:
                    jdbcTemplate.execute("UPDATE permission_audit SET is_active = TRUE WHERE is_active IS NULL");
                    break;
            }
        } catch (Exception e) {
            logger.warn("Could not update existing records: " + e.getMessage());
        }
    }
}
