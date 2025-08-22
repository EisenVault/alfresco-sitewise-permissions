#!/bin/bash

# Test script for Phase 6 filtering functionality
# This script demonstrates the new filter parameters for both JSON API and XLSX export

ALFRESCO_URL="http://localhost:8080"
ADMIN_USER="admin"
ADMIN_PASS="admin"
SITE_NAME="CRM"

echo "=== Phase 6 Filtering Test Script ==="
echo "Testing new filter parameters for permission reporting endpoints"
echo ""

# Test 1: Basic JSON API with no filters
echo "Test 1: Basic JSON API (no filters)"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME" \
     -o test1_basic.json
echo "Result saved to test1_basic.json"
echo ""

# Test 2: JSON API with user status filter (Active only)
echo "Test 2: JSON API with user status filter (Active only)"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&userStatus=Active" \
     -o test2_userstatus.json
echo "Result saved to test2_userstatus.json"
echo ""

# Test 3: JSON API with from date filter
echo "Test 3: JSON API with from date filter (permissions from 2024-01-01)"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&fromDate=2024-01-01" \
     -o test3_fromdate.json
echo "Result saved to test3_fromdate.json"
echo ""

# Test 4: JSON API with username search filter
echo "Test 4: JSON API with username search filter (searching for 'homer')"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&usernameSearch=homer" \
     -o test4_username.json
echo "Result saved to test4_username.json"
echo ""

# Test 5: JSON API with multiple filters
echo "Test 5: JSON API with multiple filters (Active users, from 2024-01-01, searching 'john')"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&userStatus=Active&fromDate=2024-01-01&usernameSearch=john" \
     -o test5_multiple.json
echo "Result saved to test5_multiple.json"
echo ""

# Test 6: XLSX export with no filters
echo "Test 6: XLSX export (no filters)"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=$SITE_NAME" \
     -o test6_basic.xlsx
echo "Result saved to test6_basic.xlsx"
echo ""

# Test 7: XLSX export with user status filter
echo "Test 7: XLSX export with user status filter (Active only)"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=$SITE_NAME&userStatus=Active" \
     -o test7_userstatus.xlsx
echo "Result saved to test7_userstatus.xlsx"
echo ""

# Test 8: XLSX export with from date filter
echo "Test 8: XLSX export with from date filter (permissions from 2024-01-01)"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=$SITE_NAME&fromDate=2024-01-01" \
     -o test8_fromdate.xlsx
echo "Result saved to test8_fromdate.xlsx"
echo ""

# Test 9: XLSX export with username search filter
echo "Test 9: XLSX export with username search filter (searching for 'john')"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=$SITE_NAME&usernameSearch=john" \
     -o test9_username.xlsx
echo "Result saved to test9_username.xlsx"
echo ""

# Test 10: XLSX export with multiple filters
echo "Test 10: XLSX export with multiple filters (Active users, from 2024-01-01, searching 'john')"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=$SITE_NAME&userStatus=Active&fromDate=2024-01-01&usernameSearch=john" \
     -o test10_multiple.xlsx
echo "Result saved to test10_multiple.xlsx"
echo ""

# Test 11: Error handling - invalid user status
echo "Test 11: Error handling - invalid user status"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&userStatus=Invalid" \
     -o test11_error_userstatus.json
echo "Result saved to test11_error_userstatus.json"
echo ""

# Test 12: Error handling - invalid date format
echo "Test 12: Error handling - invalid date format"
curl -u "$ADMIN_USER:$ADMIN_PASS" \
     "$ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&fromDate=invalid-date" \
     -o test12_error_date.json
echo "Result saved to test12_error_date.json"
echo ""

echo "=== Filtering Test Complete ==="
echo "All test results saved to files with prefix 'test*'"
echo ""
echo "Filter Parameters Summary:"
echo "- userStatus: All, Active, Inactive"
echo "- fromDate: yyyy-MM-dd format (e.g., 2024-01-01)"
echo "- usernameSearch: partial match on username or email"
echo ""
echo "Example Usage:"
echo "JSON API: $ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions?site=$SITE_NAME&userStatus=Active&fromDate=2024-01-01&usernameSearch=john"
echo "XLSX Export: $ALFRESCO_URL/alfresco/service/alfresco/tutorials/direct-permissions-xlsx?site=$SITE_NAME&userStatus=Active&fromDate=2024-01-01&usernameSearch=john"
