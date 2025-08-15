# Alfresco User Rights Report - SDK 3

This is an All-In-One (AIO) project for Alfresco SDK 3.0 that provides comprehensive user rights reporting functionality for Alfresco 5.2 CE.

## Project Overview

The Alfresco User Rights Report project is designed to create a comprehensive system for reporting and analyzing user permissions across Alfresco sites, groups, and content. The project is currently in **Phase 0** (setup complete) and provides a solid foundation for developing advanced permission reporting capabilities.

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
```

#### **Access Points**
- **Alfresco Share**: http://localhost:8080/share (admin/admin)
- **Alfresco Repository**: http://localhost:8080/alfresco
- **Web Scripts**: http://localhost:8080/alfresco/service/sample/*

#### **Test Data Setup**
The `setup-test-data.sh` script performs the following operations:
1. Creates 100+ test users across three departments
2. Creates organizational groups for each department
3. Assigns users to appropriate groups
4. Adds 35+ users per site with random roles
5. Creates baseline test content with permissions
6. Applies random local permissions to folders
7. Generates dummy PDF files with random permissions

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

### **Technical Details**

#### **Alfresco Services Used**
- `NodeService` - Content and folder management
- `SiteService` - Site operations
- `PersonService` - User management
- `AuthorityService` - Group and permission management
- `PermissionService` - Permission assignment
- `ContentService` - File content creation

#### **Web Script Endpoints**
- `POST /service/sample/create-users` - Create test users
- `POST /service/sample/create-groups` - Create test groups
- `POST /service/sample/assign-users-to-groups` - Assign users to groups
- `POST /service/sample/add-users-to-sites` - Add users to sites
- `POST /service/sample/create-test-content` - Create test content
- `POST /service/sample/add-user-to-group` - Individual user-group assignment
- `POST /service/sample/randomize-permissions-and-docs` - Apply random permissions and create PDFs

#### **Configuration**
- **Audit Enabled**: Permission changes are tracked for reporting
- **AMP Assembly**: Production-ready packaging
- **Java 7 Compatibility**: Optimized for Alfresco 5.2 CE
- **Spring Context**: Proper service injection and web script registration

### **Development Status**

#### **Completed (Phase 0)**
- ✅ Environment setup and configuration
- ✅ Test data creation and management
- ✅ Web script development for data setup
- ✅ Permission randomization and content generation
- ✅ Audit subsystem configuration
- ✅ Code cleanup and optimization
- ✅ Apache 2.0 license implementation

#### **Ready for Phase 1**
- Direct Permission Listing (No Groups)
- Web script development for permission reporting
- JSON API endpoint creation for User Rights Report
- Share UI development for report visualization

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

### **License**

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

### **Contributing**

This project follows standard Alfresco development practices:
- Java 7 compatibility
- Spring-based architecture
- Maven build system
- Web script development patterns
- Integration testing framework

### **Support**

For issues and questions:
- Check the [PHASE_0_SETUP.md](PHASE_0_SETUP.md) for detailed setup information
- Review the web script endpoints for API documentation
- Examine the test data structure for understanding the data model