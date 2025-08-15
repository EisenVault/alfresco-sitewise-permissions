#!/bin/bash

# Test Script for Direct Permissions Web Script

# Configuration
ALFRESCO_URL="http://localhost:8080/alfresco"
ADMIN_USER="admin"
ADMIN_PASS="admin"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Function to test direct permissions web script
test_direct_permissions() {
    local site=$1
    print_status "Testing direct permissions for site: $site"
    
    response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" \
        "$ALFRESCO_URL/service/sample/direct-permissions?site=$site")
    
    if [ $? -eq 0 ]; then
        print_status "Response received for site $site:"
        
        # Try to parse as JSON first
        if echo "$response" | jq '.' >/dev/null 2>&1; then
            echo "$response" | jq '.'
            
            # Extract some key metrics
            success=$(echo "$response" | jq -r '.success' 2>/dev/null)
            totalNodes=$(echo "$response" | jq -r '.totalNodes' 2>/dev/null)
            totalPermissions=$(echo "$response" | jq -r '.totalPermissions' 2>/dev/null)
            userPermissions=$(echo "$response" | jq -r '.userPermissions' 2>/dev/null)
            groupPermissions=$(echo "$response" | jq -r '.groupPermissions' 2>/dev/null)
            effectivePermissions=$(echo "$response" | jq -r '.effectivePermissions' 2>/dev/null)
        else
            print_warning "Response is not valid JSON, showing raw response:"
            echo "$response"
            success="false"
        fi
        
        if [ "$success" = "true" ]; then
            print_status "✅ Success! Site: $site"
            print_status "   Total Nodes: $totalNodes"
            print_status "   Total Permissions: $totalPermissions"
            print_status "   User Permissions: $userPermissions"
            print_status "   Group Permissions: $groupPermissions"
            print_status "   Effective Permissions: $effectivePermissions"
        else
            error=$(echo "$response" | jq -r '.error' 2>/dev/null)
            if [ "$error" = "null" ] || [ -z "$error" ]; then
                print_error "❌ Failed for site $site: Unknown error (check response above)"
                print_error "   Response: $response"
            else
                print_error "❌ Failed for site $site: $error"
            fi
        fi
    else
        print_error "❌ Failed to connect to Alfresco for site $site"
    fi
    echo
}

# Check if Alfresco is running
print_status "Checking if Alfresco is running..."
if ! curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" > /dev/null; then
    print_error "Alfresco is not running or not accessible at $ALFRESCO_URL"
    print_error "Please start Alfresco first with: mvn alfresco:run"
    exit 1
fi

print_status "Alfresco is running. Testing direct permissions web script..."

# Test each site
for site in crm hr finance; do
    test_direct_permissions "$site"
done

print_status "Direct permissions testing completed!"
print_status ""
print_status "To test manually with cURL:"
print_status "curl -u admin:admin 'http://localhost:8080/alfresco/service/sample/direct-permissions?site=crm'"
print_status ""
print_status "To test with Postman:"
print_status "GET http://localhost:8080/alfresco/service/sample/direct-permissions?site=crm"
print_status "Headers: Authorization: Basic YWRtaW46YWRtaW4="
