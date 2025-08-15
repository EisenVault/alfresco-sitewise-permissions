#!/bin/bash

# Diagnostic Script for Finance Site

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

print_status "Diagnosing finance site issues..."

# Check if Alfresco is running
print_status "1. Checking if Alfresco is running..."
if ! curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" > /dev/null; then
    print_error "Alfresco is not running!"
    exit 1
fi
print_status "✅ Alfresco is running"

# Check if finance site exists
print_status "2. Checking if finance site exists..."
finance_site_response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/sites/finance")
echo "Finance site response: $finance_site_response"

# Check all sites
print_status "3. Listing all available sites..."
sites_response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/sites")
echo "All sites: $sites_response"

# Test direct permissions with verbose output
print_status "4. Testing direct permissions with verbose output..."
direct_perms_response=$(curl -v -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/sample/direct-permissions?site=finance" 2>&1)
echo "Verbose response:"
echo "$direct_perms_response"

# Test with a working site for comparison
print_status "5. Testing with CRM site for comparison..."
crm_response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/sample/direct-permissions?site=crm")
echo "CRM response: $crm_response"

print_status "Diagnosis complete!"
