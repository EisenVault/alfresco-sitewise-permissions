# Alfresco User Rights Report - Phase 5 Complete

A comprehensive User Rights Report system for Alfresco 5.2 CE that provides detailed insights into active and inactive user permissions, with site-wise reporting and inclusion of access granted directly or via groups.

## **Project Status: Phase 5 Complete** ✅

This project implements a complete permission auditing and reporting system for Alfresco 5.2 CE, including:

- **Permission Auditing**: Custom database table for tracking permission changes
- **Comprehensive Reporting**: XLSX export with detailed permission information
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

### **Manual Permission Scanning**
- **On-Demand Execution**: Web script for manual permission audits
- **Comprehensive Coverage**: Scans all sites and document libraries
- **Detailed Logging**: Provides detailed execution logs and statistics

## **Web Script Endpoints**

### **Permission Reports**
- `GET /alfresco/service/alfresco/tutorials/direct-permissions?site={siteName}` - Get all permissions (direct + group-based) for a site
- `GET /alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site={siteName}` - Export comprehensive permission report as XLSX file

### **Permission Scanning**
- `GET /alfresco/service/alfresco/tutorials/permission-checker?action=check-permissions` - Manually trigger comprehensive permission scan

## **XLSX Report Columns**

1. **Username** - System username of the user
2. **Site** - Site to which the user belongs
3. **Node Name** - Display name of the folder or document
4. **Current Role / Permission Status** - Manager, Collaborator, Contributor, Consumer, or Viewer
5. **From Date** - Permission start date (from audit data or node creation date)
6. **User Status** - Active or Disabled status
7. **User Login** - Last login date/time from audit logs
8. **Group Name** - Group name if permission comes via a group
9. **NodeRef** - Alfresco NodeRef identifier for the node
10. **Node Type** - Alfresco content type (e.g., cm:folder, cm:content, or custom document types)
11. **Document Path** - Full path to the document/folder

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

2. **Test permission scanning**:
```bash
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/permission-checker?action=check-permissions"
```

3. **Download XLSX report**:
```bash
curl -u admin:admin "http://localhost:8080/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=test-site" -o permissions-report.xlsx
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
1. Modify `DirectPermissionsXlsxWebScript.java`
2. Update headers array and data mapping
3. Add corresponding helper methods
4. Update README documentation

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

---

**Version**: 1.0-SNAPSHOT  
**Compatibility**: Alfresco 5.2 CE  
**License**: Apache License 2.0  
**Support**: See troubleshooting section above