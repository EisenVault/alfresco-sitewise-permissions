#!/bin/bash

# Test Script for User Info Web Script

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

print_status "Testing User Info Web Script..."

# Check if Alfresco is running
print_status "1. Checking if Alfresco is running..."
if ! curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" > /dev/null; then
    print_error "Alfresco is not running!"
    exit 1
fi
print_status "✅ Alfresco is running"

# Test cases
test_cases=(
    "admin"
    "john.doe"
    "jane.smith"
    "nonexistent.user"
)

print_header "2. Testing User Info Web Script"

for username in "${test_cases[@]}"; do
    print_status "Testing user: $username"
    
    response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/sample/user-info?username=$username")
    
    if [ $? -eq 0 ]; then
        print_status "Response received for user $username:"
        
        # Try to parse as JSON first
        if echo "$response" | jq '.' >/dev/null 2>&1; then
            echo "$response" | jq '.'
            
            # Extract some key metrics
            success=$(echo "$response" | jq -r '.success' 2>/dev/null)
            totalUsers=$(echo "$response" | jq -r '.totalUsers' 2>/dev/null)
            
            if [ "$success" = "true" ]; then
                print_status "✅ Success! User: $username"
                print_status "   Total Users: $totalUsers"
                
                # Extract user details
                if [ "$totalUsers" -gt 0 ]; then
                    userStatus=$(echo "$response" | jq -r '.users[0].status' 2>/dev/null)
                    lastLogin=$(echo "$response" | jq -r '.users[0].lastLogin' 2>/dev/null)
                    firstName=$(echo "$response" | jq -r '.users[0].firstName' 2>/dev/null)
                    lastName=$(echo "$response" | jq -r '.users[0].lastName' 2>/dev/null)
                    email=$(echo "$response" | jq -r '.users[0].email' 2>/dev/null)
                    
                    print_status "   Status: $userStatus"
                    print_status "   Last Login: $lastLogin"
                    print_status "   Name: $firstName $lastName"
                    print_status "   Email: $email"
                fi
            else
                error=$(echo "$response" | jq -r '.error' 2>/dev/null)
                if [ "$error" = "null" ] || [ -z "$error" ]; then
                    print_error "❌ Failed for user $username: Unknown error"
                else
                    print_error "❌ Failed for user $username: $error"
                fi
            fi
        else
            print_warning "Response is not valid JSON, showing raw response:"
            echo "$response"
        fi
    else
        print_error "❌ Failed to get response for user $username"
    fi
    
    echo
done

print_header "3. Testing Status Filtering"

# Test with status filter
print_status "Testing with status=active filter..."
response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/sample/user-info?username=admin&status=active")

if [ $? -eq 0 ]; then
    print_status "Response received for status=active filter:"
    if echo "$response" | jq '.' >/dev/null 2>&1; then
        echo "$response" | jq '.'
        
        success=$(echo "$response" | jq -r '.success' 2>/dev/null)
        statusFilter=$(echo "$response" | jq -r '.statusFilter' 2>/dev/null)
        
        if [ "$success" = "true" ]; then
            print_status "✅ Success! Status Filter: $statusFilter"
        else
            error=$(echo "$response" | jq -r '.error' 2>/dev/null)
            print_error "❌ Failed: $error"
        fi
    else
        print_warning "Response is not valid JSON"
        echo "$response"
    fi
else
    print_error "❌ Failed to get response for status filter"
fi

echo

print_header "4. Testing Error Cases"

# Test without username parameter
print_status "Testing without username parameter (should fail)..."
response=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/sample/user-info")

if [ $? -eq 0 ]; then
    print_status "Response received:"
    if echo "$response" | jq '.' >/dev/null 2>&1; then
        echo "$response" | jq '.'
        
        success=$(echo "$response" | jq -r '.success' 2>/dev/null)
        if [ "$success" = "false" ]; then
            error=$(echo "$response" | jq -r '.error' 2>/dev/null)
            print_status "✅ Correctly failed with error: $error"
        else
            print_warning "⚠️  Expected failure but got success"
        fi
    else
        print_warning "Response is not valid JSON"
        echo "$response"
    fi
else
    print_error "❌ Failed to get response"
fi

echo

print_status "User Info testing completed!"
print_status ""
print_status "To test manually with cURL:"
print_status "curl -u \$ALFRESCO_ADMIN_USER:\$ALFRESCO_ADMIN_PASS '$ALFRESCO_URL/service/sample/user-info?username=admin'"
print_status ""
print_status "To test with Postman:"
print_status "GET http://localhost:8080/alfresco/service/sample/user-info?username=admin"
print_status "Headers: Authorization: Basic YWRtaW46YWRtaW4="
