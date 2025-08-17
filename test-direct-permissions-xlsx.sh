#!/bin/bash

# Test Script for Direct Permissions XLSX Export Web Script

# Configuration
ALFRESCO_URL="${ALFRESCO_URL:-http://localhost:8080/alfresco}"
ADMIN_USER="${ALFRESCO_ADMIN_USER:-admin}"
ADMIN_PASS="${ALFRESCO_ADMIN_PASS:-admin}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[HEADER]${NC} $1"
}

# Security check - warn if using default credentials
if [ "$ALFRESCO_ADMIN_USER" = "" ] || [ "$ALFRESCO_ADMIN_PASS" = "" ]; then
    print_warning "Using default Alfresco credentials. For production, set ALFRESCO_ADMIN_USER and ALFRESCO_ADMIN_PASS environment variables."
fi

# Function to test XLSX export web script
test_xlsx_export() {
    local site=$1
    local output_file="permissions_${site}_$(date +%Y%m%d_%H%M%S).xlsx"
    
    print_status "Testing XLSX export for site: $site"
    print_status "Output file: $output_file"
    
    response=$(curl -s -w "%{http_code}" -u "$ADMIN_USER:$ADMIN_PASS" \
        -H "Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
        "$ALFRESCO_URL/service/sample/direct-permissions-xlsx?site=$site" \
        -o "$output_file")
    
    http_code="${response: -3}"
    
    if [ "$http_code" = "200" ]; then
        print_status "✅ XLSX export successful for site $site"
        
        # Check if file was created and has content
        if [ -f "$output_file" ] && [ -s "$output_file" ]; then
            print_status "✅ XLSX file created: $output_file"
            
            # Get file size
            file_size=$(stat -f%z "$output_file" 2>/dev/null || stat -c%s "$output_file" 2>/dev/null || echo "unknown")
            print_status "   File size: $file_size bytes"
            
            # Check if it's a valid XLSX file (should start with PK)
            if file "$output_file" | grep -q "Zip archive"; then
                print_status "✅ File is a valid XLSX/ZIP archive"
            else
                print_warning "⚠️  File may not be a valid XLSX file"
            fi
            
        else
            print_error "❌ XLSX file is empty or was not created"
        fi
    else
        print_error "❌ Failed to export XLSX for site $site (HTTP $http_code)"
        if [ -f "$output_file" ]; then
            print_error "Error response:"
            cat "$output_file"
            rm -f "$output_file"
        fi
    fi
    echo
}

# Function to validate XLSX content (basic check)
validate_xlsx_content() {
    local xlsx_file=$1
    local site=$2
    
    print_status "Validating XLSX content for $site..."
    
    if [ ! -f "$xlsx_file" ]; then
        print_error "❌ XLSX file not found: $xlsx_file"
        return 1
    fi
    
    # Check if we have unzip available to peek into the XLSX file
    if command -v unzip >/dev/null 2>&1; then
        print_status "Checking XLSX file structure..."
        
        # List contents of the XLSX file (it's a ZIP archive)
        if unzip -l "$xlsx_file" | grep -q "xl/worksheets/sheet1.xml"; then
            print_status "✅ XLSX file contains expected worksheet"
        else
            print_warning "⚠️  XLSX file structure may be incomplete"
        fi
    else
        print_warning "⚠️  unzip not available, skipping XLSX content validation"
    fi
}

# Check if Alfresco is running
print_status "Checking if Alfresco is running..."
if ! curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" > /dev/null; then
    print_error "Alfresco is not running or not accessible at $ALFRESCO_URL"
    print_error "Please start Alfresco first with: mvn alfresco:run"
    exit 1
fi

print_status "Alfresco is running. Testing XLSX export web script..."

# Test each site
for site in crm hr finance; do
    test_xlsx_export "$site"
    
    # Validate the generated XLSX file
    xlsx_file="permissions_${site}_$(ls -t permissions_${site}_*.xlsx 2>/dev/null | head -1)"
    if [ -n "$xlsx_file" ] && [ -f "$xlsx_file" ]; then
        validate_xlsx_content "$xlsx_file" "$site"
    fi
    echo
done

print_status "XLSX export testing completed!"
print_status ""
print_status "To test manually with cURL:"
print_status "curl -u \$ALFRESCO_ADMIN_USER:\$ALFRESCO_ADMIN_PASS -H 'Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' '$ALFRESCO_URL/service/sample/direct-permissions-xlsx?site=crm' -o permissions_crm.xlsx"
print_status ""
print_status "To test with browser:"
print_status "Visit: http://localhost:8080/alfresco/service/sample/direct-permissions-xlsx?site=crm"
print_status "Login with admin/admin when prompted"
print_status ""
print_status "Generated XLSX files:"
ls -la permissions_*.xlsx 2>/dev/null || print_status "No XLSX files generated"
