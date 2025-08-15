#!/bin/bash

# Check Sites Structure Script

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

print_status "Checking sites structure..."

# Check if Alfresco is running
if ! curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" > /dev/null; then
    print_error "Alfresco is not running!"
    exit 1
fi

# Check each site
for site in crm hr finance; do
    print_status "Checking site: $site"
    
    # Check if site exists
    site_response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/sites/$site")
    if echo "$site_response" | grep -q "not found\|error"; then
        print_error "Site $site does not exist!"
        continue
    fi
    
    print_status "✅ Site $site exists"
    
    # Check document library
    doclib_response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/sites/$site/containers/documentLibrary")
    if echo "$doclib_response" | grep -q "not found\|error"; then
        print_warning "⚠️  Site $site does not have documentLibrary container"
    else
        print_status "✅ Site $site has documentLibrary container"
    fi
    
    # Check site members
    members_response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/sites/$site/members")
    member_count=$(echo "$members_response" | jq '.data | length' 2>/dev/null || echo "0")
    print_status "   Members: $member_count"
    
    echo
done

print_status "Sites check complete!"
