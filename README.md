# Alfresco User Rights Report - SDK 3

This is an All-In-One (AIO) project for Alfresco SDK 3.0 that provides comprehensive user rights reporting functionality for Alfresco 5.2 CE.

## Project Overview

The Alfresco User Rights Report project is designed to create a comprehensive system for reporting and analyzing user permissions across Alfresco sites, groups, and content. The project has completed **Phase 0** (setup), **Phase 1** (direct permission listing), **Phase 2** (group expansion), **Phase 3** (user status & login data), **Phase 4** (XLSX export with audit integration), and **Phase 5** (permission auditing), providing a complete permission reporting system with audit tracking capabilities.

## Features Implemented

### ✅ Phase 0 - Environment Setup (Complete)

#### **Core Infrastructure**
- **Alfresco SDK 3.0 Environment**: Full development environment with embedded Tomcat, H2 database, Solr4, and Share
- **AMP Assembly Plugin**: Enabled for production deployment packaging
- **Audit Subsystem**: Configured for permission change tracking
- **Maven Build System**: Standard JAR packaging with AMP assembly support

#### **Test Data Management**
- **Comprehensive Test Data Creation**: 100+ realistic users across CRM, HR, and Finance departments
- **Organizational Structure**: Department-specific groups with proper hierarchy
- **Site Management**: Automated site membership with random role assignments
- **Content Generation**: Structured folders with specific permissions
- **Permission Randomization**: Local permissions on folders and documents with dummy PDF generation

#### **Web Scripts Implemented**
1. **`CreateUsersWebScript`** - Creates 100+ test users with realistic names and email addresses
2. **`CreateGroupsWebScript`** - Creates organizational groups for each department
3. **`AssignUsersToGroupsWebScript`** - Assigns users to appropriate groups within their departments
4. **`AddUsersToSitesWebScript`** - Adds users to sites with random roles (SiteManager, SiteCollaborator, etc.)
5. **`CreateTestContentWebScript`** - Creates test folders with base permissions
6. **`AddUserToGroupWebScript`** - Individual user-group assignment utility
7. **`RandomizePermissionsAndDocsWebScript`** - Applies random local permissions and generates dummy PDFs
8. **`DirectPermissionsWebScript`** - Lists all direct and group-based permissions for a selected site (Phase 1 & 2)
9. **`UserInfoWebScript`** - Retrieves user information including status and last login date (Phase 3)
10. **`DirectPermissionsXlsxWebScript`** - Exports comprehensive permission reports as XLSX files with audit data (Phase 4 & 5)

#### **Test Data Structure**
- **Sites**: CRM, HR, Finance (3 main sites with Document Library)
- **Users**: 100+ realistic users distributed across departments
- **Groups**: Department-specific groups (CRM_Managers, HR_Users, Finance_Accounting_Team, etc.)
- **Content**: Structured folders with specific permissions per group
- **Roles**: SiteManager, SiteCollaborator, SiteContributor, SiteConsumer
- **Permissions**: Read, Write, Delete, CreateChildren with random assignment

### **Setup and Usage**

#### **Prerequisites**
- Java 7 or higher
- Maven 3.x
- Alfresco 5.2 CE (embedded in project)

#### **Quick Start**
```bash
# Build the project
mvn clean install -DskipTests=true

# Start Alfresco
mvn alfresco:run
# or
./run.sh

# Setup test data
./setup-test-data.sh

# Test permission reporting
./test-direct-permissions.sh
```

#### **Security Configuration**
The test scripts use environment variables for Alfresco credentials to avoid hardcoding sensitive information:

```bash
# Set environment variables (recommended for production)
export ALFRESCO_ADMIN_USER=your_admin_username
export ALFRESCO_ADMIN_PASS=your_secure_password
export ALFRESCO_URL=http://localhost:8080/alfresco

# Or use the example configuration
cp env.example .env
# Edit .env with your actual credentials
```

**⚠️ Security Warning**: 
- Never commit actual credentials to version control
- The default `admin:admin` credentials are for development only
- Use strong, unique passwords in production environments
- Consider using Alfresco's authentication mechanisms for production deployments

#### **Access Points**
- **Alfresco Share**: http://localhost:8080/share (admin/admin)
- **Alfresco Repository**: http://localhost:8080/alfresco
- **Web Scripts**: http://localhost:8080/alfresco/service/sample/*
- **Permission Report API**: http://localhost:8080/alfresco/service/sample/direct-permissions?site={siteName}

#### **Test Data Setup**
The `setup-test-data.sh` script performs the following operations:
1. Creates 100+ test users across three departments
2. Creates organizational groups for each department
3. Assigns users to appropriate groups
4. Adds 35+ users per site with random roles
5. Creates baseline test content with permissions
6. Applies random local permissions to folders
7. Generates dummy PDF files with random permissions

#### **Permission Reporting**
The `test-direct-permissions.sh` script tests the permission reporting functionality:
1. Tests direct permissions for CRM, HR, and Finance sites
2. Validates JSON response format and data integrity
3. Provides detailed metrics (total nodes, permissions, user vs group permissions)
4. Includes error handling and diagnostic information

### **Project Architecture**

#### **Module Structure**
```
sitewise-permissions/
├── sitewise-permissions-platform-jar/    # Repository/Platform module
├── sitewise-permissions-share-jar/       # Share UI module
├── integration-tests/                    # Test framework
└── setup-test-data.sh                   # Test data setup script
```

#### **Key Components**
- **Platform Module**: Java web scripts, Alfresco services integration
- **Share Module**: Frontend UI components (ready for Phase 1 development)
- **Integration Tests**: Test framework for validation
- **Audit Configuration**: Permission change tracking enabled
- **Custom Database**: Permission audit table for tracking permission changes

### **Technical Details**

#### **Alfresco Services Used**
- `NodeService` - Content and folder management
- `SiteService` - Site operations
- `PersonService` - User management
- `AuthorityService` - Group and permission management
- `PermissionService` - Permission assignment
- `ContentService` - File content creation
- `AuditService` - Audit log querying for login data
- `PolicyComponent` - Permission change policy registration
- `JdbcTemplate` - Database operations for permission auditing

#### **Web Script Endpoints**
- `POST /service/sample/create-users` - Create test users
- `POST /service/sample/create-groups` - Create test groups
- `POST /service/sample/assign-users-to-groups` - Assign users to groups
- `POST /service/sample/add-users-to-sites` - Add users to sites
- `POST /service/sample/create-test-content` - Create test content
- `POST /service/sample/add-user-to-group` - Individual user-group assignment
- `POST /service/sample/randomize-permissions-and-docs` - Apply random permissions and create PDFs
- `GET /service/sample/direct-permissions?site={siteName}` - Get all permissions (direct + group-based) for a site
- `GET /service/sample/user-info?username={username}&status={filter}` - Get user information with status filtering
- `GET /service/sample/direct-permissions-xlsx?site={siteName}` - Export comprehensive permission report as XLSX file with audit data

#### **Configuration**
- **Audit Enabled**: Permission changes are tracked for reporting
- **Login Audit Integration**: Real login data retrieval from Alfresco audit logs
- **Permission Audit Integration**: Custom database table for tracking permission grants/revokes
- **Apache POI**: Excel file generation with professional formatting
- **AMP Assembly**: Production-ready packaging
- **Java 7 Compatibility**: Optimized for Alfresco 5.2 CE
- **Spring Context**: Proper service injection and web script registration
- **Database Integration**: Automatic table creation and permission tracking

### **Development Status**

#### **Completed (Phase 0)**
- ✅ Environment setup and configuration
- ✅ Test data creation and management
- ✅ Web script development for data setup
- ✅ Permission randomization and content generation
- ✅ Audit subsystem configuration
- ✅ Code cleanup and optimization
- ✅ Apache 2.0 license implementation

#### **Completed (Phase 1)**
- ✅ Direct Permission Listing (No Groups)
- ✅ Web script development for permission reporting
- ✅ JSON API endpoint creation for User Rights Report
- ✅ Permission filtering and data structure implementation

#### **Completed (Phase 2)**
- ✅ Group Expansion functionality
- ✅ Recursive group membership resolution
- ✅ Infinite loop prevention in group expansion
- ✅ Combined direct and group-based permission reporting
- ✅ Enhanced JSON response with permission type indicators

#### **Completed (Phase 3)**
- ✅ User Status & Last Login functionality
- ✅ Separate UserInfoWebScript for user information
- ✅ Status filtering (active/inactive/all)
- ✅ User information retrieval with error handling
- ✅ Modular design with single responsibility principle

#### **Completed (Phase 4)**
- ✅ XLSX Export functionality with professional formatting
- ✅ Audit integration for real login data retrieval
- ✅ Comprehensive permission reporting with all required columns
- ✅ Apache POI integration for Excel file generation
- ✅ LoginAuditService for querying Alfresco audit logs
- ✅ Detailed logging for audit query debugging
- ✅ Professional Excel formatting with headers, borders, and auto-sizing

#### **Completed (Phase 5)**
- ✅ Permission Auditing system with custom database table
- ✅ PermissionAuditService for recording and querying permission changes
- ✅ PermissionChangePolicy for intercepting permission updates
- ✅ DatabaseInitializer for automatic table creation
- ✅ Enhanced XLSX export with audit data integration
- ✅ "Permission Given By", "From Date", and "To Date" columns with real audit data
- ✅ Fallback mechanisms for pre-implementation permissions
- ✅ Transactional database operations with proper error handling

#### **Ready for Future Phases**
- Share UI development for report visualization
- Advanced filtering and search capabilities
- PDF export functionality
- Real-time permission monitoring
- Enhanced policy integration for automatic permission tracking

### **Build and Deployment**

#### **Development Build**
```bash
mvn clean compile -DskipTests=true
```

#### **Full Build with AMP**
```bash
mvn clean install -DskipTests=true
```

#### **Production Deployment**
The project generates AMP files for deployment:
- `sitewise-permissions-platform-jar-1.0-SNAPSHOT.amp`
- `sitewise-permissions-share-jar-1.0-SNAPSHOT.amp`

**Database Setup**: The permission audit table is automatically created on module startup - no manual SQL execution required.

### **License**

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

### **Contributing**

This project follows standard Alfresco development practices:
- Java 7 compatibility
- Spring-based architecture
- Maven build system
- Web script development patterns
- Integration testing framework

### **API Documentation**

#### **Permission Report Endpoint**
```
GET /service/sample/direct-permissions?site={siteName}
```

**Parameters:**
- `site` (required): The short name of the site (e.g., "crm", "hr", "finance")

**Response Format:**
```json
{
  "success": true,
  "site": "crm",
  "totalNodes": 15,
  "totalPermissions": 45,
  "userPermissions": 20,
  "groupPermissions": 25,
  "effectivePermissions": 120,
  "permissions": [
    {
      "username": "john.doe",
      "nodePath": "/Company Home/Sites/crm/documentLibrary/Projects",
      "role": "SiteManager",
      "nodeName": "Project Alpha",
      "nodeType": "{http://www.alfresco.org/model/content/1.0}folder",
      "groupName": "",
      "permissionType": "DIRECT"
    },
    {
      "username": "jane.smith",
      "nodePath": "/Company Home/Sites/crm/documentLibrary/Reports",
      "role": "SiteCollaborator",
      "nodeName": "Q1 Report.pdf",
      "nodeType": "{http://www.alfresco.org/model/content/1.0}content",
      "groupName": "GROUP_site_crm_SiteCollaborator",
      "permissionType": "GROUP"
    }
  ]
}
```

**Response Fields:**
- `success`: Boolean indicating if the request was successful
- `site`: The site name that was queried
- `totalNodes`: Total number of nodes (folders/files) in the site's document library
- `totalPermissions`: Total number of permission entries found
- `userPermissions`: Number of direct user permissions
- `groupPermissions`: Number of group-based permissions
- `effectivePermissions`: Total number of effective permissions after group expansion
- `permissions`: Array of permission entries with detailed information

**Permission Entry Fields:**
- `username`: The user who has the permission
- `nodePath`: Full path to the node in Alfresco
- `role`: The permission role (e.g., SiteManager, SiteCollaborator)
- `nodeName`: Name of the node (folder or file)
- `nodeType`: Alfresco content model type
- `groupName`: Group name if permission is group-based (empty for direct permissions)
- `permissionType`: Either "DIRECT" or "GROUP"

#### **User Info Endpoint**
```
GET /service/sample/user-info?username={username}&status={filter}
```

**Parameters:**
- `username` (required): The username to get information for
- `status` (optional): Filter by user status - "active", "inactive", or "all" (default: "all")

**Response Format:**
```json
{
  "success": true,
  "statusFilter": "all",
  "totalUsers": 1,
  "users": [
    {
      "username": "admin",
      "status": "active",
      "lastLogin": "Not Available",
      "firstName": "Administrator",
      "lastName": "",
      "email": "admin@alfresco.com"
    }
  ]
}
```

**Response Fields:**
- `success`: Boolean indicating if the request was successful
- `statusFilter`: The status filter that was applied
- `totalUsers`: Number of users returned
- `users`: Array of user information objects

**User Information Fields:**
- `username`: The username
- `status`: User status ("active", "inactive", or "unknown")
- `lastLogin`: Last login date or "Not Available"
- `firstName`: User's first name
- `lastName`: User's last name
- `email`: User's email address

#### **XLSX Export Endpoint**
```
GET /service/sample/direct-permissions-xlsx?site={siteName}
```

**Parameters:**
- `site` (required): The short name of the site (e.g., "crm", "hr", "finance")

**Response:**
- **Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **File Format**: XLSX (Excel) file with professional formatting
- **Download**: Browser will automatically download the file

**XLSX Report Columns:**
1. **Username** - System username of the user
2. **Site** - Site to which the user belongs
3. **Document Path** - Full path to the document/folder
4. **Current Role / Permission Status** - Manager, Collaborator, Contributor, Consumer, or Viewer
5. **Permission Given By** - Username who granted the permission (from audit data or "System")
6. **From Date** - Permission start date (from audit data or node creation date)
7. **To Date** - Permission end date (from audit data, empty if active)
8. **User Status** - Active or Disabled status
9. **User Login** - Last login date/time from audit logs
10. **Group Name** - Group name if permission comes via a group

**Features:**
- **Professional Formatting**: Headers with blue background, borders, and auto-sized columns
- **Real Login Data**: Queries Alfresco audit logs for actual login timestamps
- **Permission Audit Data**: Uses custom database table for "Permission Given By", "From Date", and "To Date"
- **Comprehensive Data**: Includes both direct and group-based permissions
- **Audit Integration**: Uses `LoginAuditService` to query `alfresco-access` audit application
- **Permission Tracking**: Uses `PermissionAuditService` for permission grant/revoke history
- **Error Handling**: Graceful fallbacks for missing audit data
- **Performance Optimized**: Efficient audit querying with unlimited results
- **Database Integration**: Automatic table creation and permission change tracking

### **Permission Auditing System (Phase 5)**

#### **Database Schema**
The system automatically creates a `permission_audit` table with the following structure:
```sql
CREATE TABLE permission_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_ref VARCHAR(255) NOT NULL,
    user_granted_to VARCHAR(255) NOT NULL,
    granted_by VARCHAR(255) NOT NULL,
    date_granted TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP NULL,
    permission VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- 'GRANT' or 'REVOKE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_node_ref (node_ref),
    INDEX idx_user_granted_to (user_granted_to),
    INDEX idx_granted_by (granted_by),
    INDEX idx_date_granted (date_granted),
    INDEX idx_action_type (action_type)
);
```

#### **Key Components**
- **`PermissionAuditService`**: Records and queries permission changes
- **`PermissionChangePolicy`**: Intercepts permission updates (manual tracking mode)
- **`DatabaseInitializer`**: Automatically creates audit table on startup
- **Enhanced XLSX Export**: Integrates audit data for comprehensive reporting

#### **Audit Data Sources**
- **"Permission Given By"**: From authentication context when permission was granted
- **"From Date"**: Date when permission was first granted (from audit table)
- **"To Date"**: Expiry date if specified, or empty for active permissions
- **Fallback Data**: Node creation date and "System" for pre-implementation permissions

### **Testing and Diagnostics**

#### **Test Scripts**
- `test-direct-permissions.sh` - Comprehensive testing of permission reporting
- `test-user-info.sh` - Testing of user information retrieval and status filtering
- `test-direct-permissions-xlsx.sh` - Testing of XLSX export functionality with audit data
- `check-sites.sh` - Verify site structure and availability
- `diagnose-finance-site.sh` - Detailed diagnostics for specific site issues

#### **Manual Testing**
```bash
# Test permissions with cURL
curl -u admin:admin 'http://localhost:8080/alfresco/service/sample/direct-permissions?site=crm'

# Test user info with cURL
curl -u admin:admin 'http://localhost:8080/alfresco/service/sample/user-info?username=admin&status=active'

# Test XLSX export with cURL
curl -u admin:admin 'http://localhost:8080/alfresco/service/sample/direct-permissions-xlsx?site=finance' -o permissions-report.xlsx

# Test with Postman
GET http://localhost:8080/alfresco/service/sample/direct-permissions?site=crm
Headers: Authorization: Basic YWRtaW46YWRtaW4=

GET http://localhost:8080/alfresco/service/sample/user-info?username=admin&status=active
Headers: Authorization: Basic YWRtaW46YWRtaW4=

GET http://localhost:8080/alfresco/service/sample/direct-permissions-xlsx?site=finance
Headers: Authorization: Basic YWRtaW46YWRtaW4=
```

### **Support**

For issues and questions:
- Check the [PHASE_0_SETUP.md](PHASE_0_SETUP.md) for detailed setup information
- Review the web script endpoints for API documentation
- Examine the test data structure for understanding the data model
- Use the diagnostic scripts for troubleshooting
- Check Alfresco logs for audit integration and database initialization messages