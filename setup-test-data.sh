#!/bin/bash

# Test Data Setup Script for User Rights Report Development

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

# Function to make REST API calls
make_api_call() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    print_status "Creating $description..."
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "%{http_code}" -u "$ADMIN_USER:$ADMIN_PASS" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url")
    else
        response=$(curl -s -w "%{http_code}" -u "$ADMIN_USER:$ADMIN_PASS" -X "$method" \
            "$url")
    fi
    
    http_code="${response: -3}"
    response_body="${response%???}"
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        print_status "$description created successfully"
        echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
    else
        print_error "Failed to create $description (HTTP $http_code)"
        echo "$response_body"
    fi
    echo
}

# Helpers
rand_role() {
    local roles=("SiteManager" "SiteCollaborator" "SiteContributor" "SiteConsumer")
    echo "${roles[$RANDOM % ${#roles[@]}]}"
}

fetch_all_usernames() {
    # returns newline-delimited usernames (excludes admin/guest)
    curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" \
      | jq -r '.people[].userName' \
      | grep -Ev '^(admin|guest)$'
}

shuffle_lines() {
    # portable shuffle using awk+sort (works on macOS without shuf)
    awk 'BEGIN{srand()} {print rand() "\t" $0}' \
    | sort -k1,1n \
    | cut -f2-
}

add_member_to_site() {
    local site=$1
    local user=$2
    local role=$3
    local payload
    payload=$(printf '{"role":"%s","person":{"userName":"%s"}}' "$role" "$user")
    make_api_call "POST" "$ALFRESCO_URL/service/api/sites/$site/memberships" "$payload" "site member $user → $site ($role)"
}

ensure_min_members() {
    local site=$1
    local min=$2

    print_status "Ensuring at least $min members in site '$site' with random roles..."
    local users
    users=$(fetch_all_usernames | shuffle_lines | head -n $min)

    local count=0
    while IFS= read -r u; do
        [ -z "$u" ] && continue
        role=$(rand_role)
        add_member_to_site "$site" "$u" "$role"
        count=$((count+1))
    done <<< "$users"
    print_status "Attempted to add $count users to '$site'."
}

randomize_permissions_and_docs_for_site() {
    local site=$1
    local docs_per_folder_min=${2:-2}
    local docs_per_folder_max=${3:-5}

    # Calls custom repo web script that:
    # - sets random local permissions on site folders
    # - generates and uploads dummy PDFs with different names
    # - sets random local permissions on those PDFs
    # Parameters are optional; sensible defaults are applied server-side as well.
    local qs="?site=$site&docsPerFolderMin=$docs_per_folder_min&docsPerFolderMax=$docs_per_folder_max"
    make_api_call "POST" "$ALFRESCO_URL/service/sample/randomize-permissions-and-docs$qs" "" "random local permissions + dummy PDFs for '$site'"
}

# Check if Alfresco is running
print_status "Checking if Alfresco is running..."
if ! curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" > /dev/null; then
    print_error "Alfresco is not running or not accessible at $ALFRESCO_URL"
    print_error "Please start Alfresco first with: mvn alfresco:run"
    exit 1
fi

print_status "Alfresco is running. Starting test data setup..."

# Note: Sites (crm, hr, finance) should be created manually in Alfresco Share
print_status "Sites should be created manually: crm, hr, finance"
print_status "Proceeding with user, group, and content creation..."

# Create test users
print_status "Creating test users..."
make_api_call "POST" "$ALFRESCO_URL/service/sample/create-users" "" "test users"

# Create test groups
print_status "Creating test groups..."
make_api_call "POST" "$ALFRESCO_URL/service/sample/create-groups" "" "test groups"

# Assign users to groups (idempotent on re-run)
print_status "Assigning users to groups..."
make_api_call "POST" "$ALFRESCO_URL/service/sample/assign-users-to-groups" "" "user-group assignments"

# Ensure 35 random users per site with random roles
for site in crm hr finance; do
    ensure_min_members "$site" 35
done

# Create baseline folders and apply base permissions (idempotent; will skip existing)
print_status "Creating test content and setting base permissions..."
make_api_call "POST" "$ALFRESCO_URL/service/sample/create-test-content" "" "test content and permissions"

# Randomize local permissions on folders and upload dummy PDFs with different names and permissions
for site in crm hr finance; do
    randomize_permissions_and_docs_for_site "$site" 2 5
done

# Verify setup
print_status "Verifying test data setup..."

print_status "Created sites:"
curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/sites" | jq '.data[].shortName' 2>/dev/null || echo "Could not retrieve sites"

print_status "Created users:"
curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/people" | jq '.people[].userName' 2>/dev/null || echo "Could not retrieve users"

print_status "Created groups:"
curl -s -u "$ADMIN_USER:$ADMIN_PASS" "$ALFRESCO_URL/service/api/groups" | jq '.data[].shortName' 2>/dev/null || echo "Could not retrieve groups"

print_status "Test data setup completed!"