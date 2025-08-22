# Alfresco User Rights Report - Phase 6 Complete

A comprehensive User Rights Report system for Alfresco 5.2 CE that provides detailed insights into active and inactive user permissions, with site-wise reporting, advanced filtering capabilities, and inclusion of access granted directly or via groups.

## **Project Status: Phase 6 Complete** ✅

This project implements a complete permission auditing and reporting system for Alfresco 5.2 CE, including:

- **Permission Auditing**: Custom database table for tracking permission changes
- **Comprehensive Reporting**: XLSX export with detailed permission information
- **Advanced Filtering**: Multi-parameter filtering for user status, date ranges, and username/email search
- **Enhanced JSON API**: Complete permission data with all columns matching XLSX export
- **Manual Permission Scanning**: Web script for on-demand permission audits
- **OS-Level Scheduling**: Instructions for cron-based nightly scanning

## **Core Features**

### **Permission Auditing System**
- **Custom Database Table**: `permission_audit` table for tracking permission grants and revokes
- **Historical Tracking**: Maintains audit trail of all permission changes
- **Revocation Detection**: Identifies when permissions are removed and marks them as "Revoked"
- **Comprehensive Coverage**: Scans all sites and document libraries recursively

### **XLSX Report Generation**
- **Professional Formatting**: Headers with blue background, borders, and auto-sized columns
- **Comprehensive Data**: Includes both direct and group-based permissions
- **Audit Integration**: Uses custom database table for "From Date" and permission status
- **Real Login Data**: Queries Alfresco audit logs for actual login timestamps

### **Advanced Filtering System**
- **User Status Filtering**: Filter by "All", "Active", or "Inactive" users
- **Date Range Filtering**: Filter permissions based on grant date (yyyy-MM-dd format)
- **Username/Email Search**: Partial match search on username or email address
- **Combined Filters**: Apply multiple filters simultaneously for precise results
- **Real-time Filtering**: Instant results without requiring database queries

### **Manual Permission Scanning**
- **On-Demand Execution**: Web script for manual permission audits
- **Comprehensive Coverage**: Scans all sites and document libraries
- **Detailed Logging**: Provides detailed execution logs and statistics

## **Web Script Endpoints**

### **Permission Reports**
- `GET /alfresco/service/alfresco/tutorials/direct-permissions?site={siteName}` - Get all permissions (direct + group-based) for a site
- `GET /alfresco/service/alfresco/tutorials/direct-permissions?site={siteName}&userStatus={status}&fromDate={date}&usernameSearch={search}` - Get filtered permissions
- `GET /alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site={siteName}` - Export comprehensive permission report as XLSX file
- `GET /alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site={siteName}&userStatus={status}&fromDate={date}&usernameSearch={search}` - Export filtered permissions as XLSX

### **Permission Scanning**
- `GET /alfresco/service/alfresco/tutorials/permission-checker?action=check-permissions` - Manually trigger comprehensive permission scan

## **API Filter Parameters**

### **Available Filters**
- **`userStatus`**: Filter by user status
  - `All` (default) - Include all users
  - `Active` - Only active users
  - `Inactive` - Only inactive users
- **`fromDate`**: Filter permissions granted from this date (inclusive)
  - Format: `yyyy-MM-dd` (e.g., `2024-01-01`)
- **`usernameSearch`**: Search by username or email address
  - Partial match (case-insensitive)
  - Searches both username and email fields

### **Filter Examples**
```bash
# Get only active users
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=CRM&userStatus=Active"

# Get permissions from specific date
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=CRM&fromDate=2024-01-01"

# Search for specific user
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=CRM&usernameSearch=john"

# Combine multiple filters
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=CRM&userStatus=Active&fromDate=2024-01-01&usernameSearch=john"
```

## **Report Columns**

### **JSON API Response**
The JSON API now includes all columns that were previously only available in XLSX export:

1. **username** - System username of the user
2. **site** - Site to which the user belongs
3. **nodePath** - Full path to the document/folder
4. **role** - Current role/permission (Manager, Collaborator, Contributor, Consumer, Viewer)
5. **nodeName** - Display name of the folder or document
6. **nodeType** - Alfresco content type (e.g., cm:folder, cm:content)
7. **nodeRef** - Alfresco NodeRef identifier for the node
8. **fromDate** - Permission start date (from audit data or node creation date)
9. **userStatus** - Active or Inactive status
10. **userLogin** - Last login date/time from audit logs
11. **groupName** - Group name if permission comes via a group
12. **permissionType** - "DIRECT" or "GROUP" indicating permission source

### **XLSX Report Columns**
Same as JSON API plus additional formatting and professional styling.

## **Database Schema**

The system automatically creates a `permission_audit` table with the following structure:

```sql
CREATE TABLE permission_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_ref VARCHAR(255) NOT NULL,
    user_granted_to VARCHAR(255) NOT NULL,
    date_granted TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP NULL,
    permission VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- 'GRANT' or 'REVOKE'
    is_active BOOLEAN DEFAULT TRUE,
    revoked_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_node_ref (node_ref),
    INDEX idx_user_granted_to (user_granted_to),
    INDEX idx_date_granted (date_granted),
    INDEX idx_action_type (action_type),
    INDEX idx_is_active (is_active)
);
```

## **Scheduling Nightly Permission Scans**

Since Quartz scheduling was removed for simplicity and reliability, use OS-level cron jobs:

### **Linux/macOS Cron Setup**

1. **Create a shell script** (`/opt/alfresco/scripts/run-permission-scan.sh`):
```bash
#!/bin/bash
# Nightly permission scan script
ALFRESCO_URL="http://localhost:8080"
ADMIN_USER="admin"
ADMIN_PASS="admin"

# Run permission scan
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/permission-checker?action=check-permissions" \
     -o /var/log/alfresco/permission-scan-$(date +%Y%m%d).log 2>&1

# Optional: Send email notification
# echo "Permission scan completed at $(date)" | mail -s "Alfresco Permission Scan" admin@company.com
```

2. **Make script executable**:
```bash
chmod +x /opt/alfresco/scripts/run-permission-scan.sh
```

3. **Add to crontab** (run at 9:00 PM daily):
```bash
# Edit crontab
crontab -e

# Add this line:
0 21 * * * /opt/alfresco/scripts/run-permission-scan.sh
```

### **Windows Task Scheduler**

1. **Create batch file** (`C:\alfresco\scripts\run-permission-scan.bat`):
```batch
@echo off
set ALFRESCO_URL=http://localhost:8080
set ADMIN_USER=admin
set ADMIN_PASS=admin

curl -u "%ADMIN_USER%:%ADMIN_PASS%" "%ALFRESCO_URL%/alfresco/service/alfresco/tutorials/permission-checker?action=check-permissions" > C:\logs\permission-scan-%date:~-4,4%%date:~-10,2%%date:~-7,2%.log 2>&1
```

2. **Create scheduled task**:
   - Open Task Scheduler
   - Create Basic Task
   - Name: "Alfresco Permission Scan"
   - Trigger: Daily at 9:00 PM
   - Action: Start a program
   - Program: `C:\alfresco\scripts\run-permission-scan.bat`

## **Installation and Deployment**

### **Prerequisites**
- Alfresco 5.2 CE
- Java 8
- Maven 3.x
- Apache POI 5.2.3 (included in dependencies)

### **Build and Deploy**

1. **Build the project**:
```bash
mvn clean install -DskipTests
```

2. **Deploy to Alfresco**:
```bash
# Copy AMP files to Alfresco
cp sitewise-permissions-platform-jar/target/*.amp $ALFRESCO_HOME/amps/
cp sitewise-permissions-share-jar/target/*.amp $ALFRESCO_HOME/amps_share/

# Apply AMPs
java -jar $ALFRESCO_HOME/bin/alfresco-mmt.jar install sitewise-permissions-platform-jar-1.0-SNAPSHOT.amp $ALFRESCO_HOME/tomcat/webapps/alfresco.war
java -jar $ALFRESCO_HOME/bin/alfresco-mmt.jar install sitewise-permissions-share-jar-1.0-SNAPSHOT.amp $ALFRESCO_HOME/tomcat/webapps/share.war

# Restart Alfresco
$ALFRESCO_HOME/alfresco.sh restart
```

### **Verification**

1. **Check web script availability**:
```bash
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=test-site"
```

2. **Test filtering functionality**:
```bash
# Test user status filter
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=test-site&userStatus=Active"

# Test date filter
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=test-site&fromDate=2024-01-01"

# Test username search
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions?site=test-site&usernameSearch=admin"
```

3. **Test permission scanning**:
```bash
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/permission-checker?action=check-permissions"
```

4. **Download XLSX report**:
```bash
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=test-site" -o permissions-report.xlsx
```

5. **Test filtered XLSX export**:
```bash
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=test-site&userStatus=Active" -o filtered-permissions.xlsx
```

## **Known Limitations**

### **Permission Grant Tracking Limitation**
Due to technical constraints in Alfresco 5.2 CE, the system has the following limitation:

**"Permission Given By" Information**: The system cannot determine who originally granted permissions. This limitation exists because:

1. **Alfresco 5.2 Policy Limitations**: The `OnUpdatePermissionsPolicy` (which would capture permission changes in real-time) is not available in Alfresco 5.2 CE. It was introduced in later versions (ACS 6.x+).

2. **Manual Polling Approach**: The current implementation uses a manual polling mechanism that scans for permission changes rather than capturing them in real-time. When permissions are detected, the system has no way to determine who originally granted them.

3. **Historical Data Gap**: Permissions created before this system was implemented have no audit trail, making it impossible to determine the original grantor.

### **Permission Change Detection Limitation**
The system has a critical limitation regarding permission changes that occur between script executions:

**"Add and Remove Between Runs" Gap**: Permissions that are added and then removed between script runs will **NOT** be recorded in the audit trail.

**Example Scenario**:
- **Time 1**: Script runs, records current permissions
- **Time 2**: User adds permission → **NOT recorded** (script not running)
- **Time 3**: User removes permission → **NOT recorded** (script not running)  
- **Time 4**: Script runs again → Sees same state as Time 1, no changes detected

**Workarounds**:
- **Run the script more frequently** (e.g., every hour via cron job)
- **Upgrade to newer Alfresco version** with `OnUpdatePermissionsPolicy` for real-time tracking
- **Implement real-time event listeners** (complex, version-dependent)
- **Use Alfresco's built-in audit service** for permission changes (requires additional configuration)

## **Technical Architecture**

### **Core Components**
- **Platform Module**: Java web scripts, Alfresco services integration
- **Share Module**: Frontend UI components (ready for integration)
- **Integration Tests**: Test framework for validation
- **Audit Configuration**: Permission change tracking enabled
- **Custom Database**: Permission audit table for tracking permission changes

### **Key Services**
- **PermissionAuditService**: Manages permission audit database operations
- **PermissionChangeScheduler**: Handles comprehensive permission scanning
- **LoginAuditService**: Retrieves user login information from Alfresco audit logs
- **DatabaseInitializer**: Automatically creates and migrates audit table schema

### **Dependencies**
- **Apache POI**: Excel file generation with professional formatting
- **Spring Framework**: Dependency injection and service management
- **Alfresco SDK 3.0**: Development framework and packaging

## **Development and Customization**

### **Adding New Report Columns**
1. Modify `DirectPermissionsXlsxWebScript.java` and `DirectPermissionsWebScript.java`
2. Update headers array and data mapping
3. Add corresponding helper methods
4. Update FreeMarker template (`direct-permissions.get.json.ftl`)
5. Update README documentation

### **Customizing Permission Scanning**
1. Modify `PermissionChangeScheduler.java`
2. Adjust scanning logic and filters
3. Update logging and error handling
4. Test with manual web script execution

### **Database Schema Changes**
1. Modify `DatabaseInitializer.java`
2. Update table creation and migration logic
3. Test with fresh and existing databases
4. Update service layer accordingly

## **Troubleshooting**

### **Common Issues**

1. **Web Script Not Found (404)**:
   - Verify AMP deployment
   - Check Alfresco logs for errors
   - Ensure web script descriptors are properly configured

2. **Permission Scan Not Working**:
   - Check database connectivity
   - Verify audit table exists
   - Review Alfresco logs for errors

3. **XLSX Export Fails**:
   - Verify Apache POI dependency
   - Check file permissions
   - Review memory settings

4. **Filtering Not Working**:
   - Verify parameter names are correct (`userStatus`, `fromDate`, `usernameSearch`)
   - Check date format is `yyyy-MM-dd`
   - Ensure site name is correct
   - Review Alfresco logs for parameter parsing errors

5. **JSON Response Missing Columns**:
   - Verify FreeMarker template includes all fields
   - Check that helper methods are working correctly
   - Ensure all required services are properly injected

### **Log Locations**
- **Alfresco Logs**: `$ALFRESCO_HOME/logs/alfresco.log`
- **Tomcat Logs**: `$ALFRESCO_HOME/logs/catalina.out`
- **Permission Scan Logs**: Custom location (configured in cron script)

## **Support and Maintenance**

### **Regular Maintenance Tasks**
1. **Monitor database size**: Audit table can grow large over time
2. **Review scan logs**: Check for errors and performance issues
3. **Update cron schedules**: Adjust frequency based on requirements
4. **Backup audit data**: Include in regular database backups

### **Performance Optimization**
1. **Database indexing**: Ensure proper indexes on audit table
2. **Scan frequency**: Balance between coverage and performance
3. **Memory settings**: Adjust JVM heap for large repositories
4. **Log rotation**: Implement log rotation for scan logs

## **Phase 6 Enhancements**

### **New Features Added**
- **Advanced Filtering System**: Multi-parameter filtering for precise permission reporting
- **Enhanced JSON API**: Complete data consistency between JSON and XLSX responses
- **Improved User Experience**: Real-time filtering without database queries
- **Comprehensive Documentation**: Complete API documentation with examples

### **Technical Improvements**
- **FreeMarker Template Updates**: Enhanced JSON response template
- **Service Integration**: Improved integration with Alfresco audit services
- **Code Optimization**: Removed debug logging for production readiness
- **Error Handling**: Enhanced parameter validation and error reporting

---

**Version**: 1.0-SNAPSHOT  
**Compatibility**: Alfresco 5.2 CE  
**License**: Apache License 2.0  
**Support**: See troubleshooting section above