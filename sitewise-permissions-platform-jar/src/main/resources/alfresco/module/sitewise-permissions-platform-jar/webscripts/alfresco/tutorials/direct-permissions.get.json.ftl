{
    "success": ${success?string},
    "site": "${site}",
    "totalNodes": ${totalNodes},
    "totalPermissions": ${totalPermissions},
    "userPermissions": ${userPermissions},
    "groupPermissions": ${groupPermissions},
    "effectivePermissions": ${effectivePermissions},
    "filteredPermissions": ${filteredPermissions},
    "permissions": [
        <#list permissions as permission>
        {
            "username": "${permission.username}",
            "site": "${permission.site}",
            "nodePath": "${permission.nodePath}",
            "role": "${permission.role}",
            "nodeName": "${permission.nodeName}",
            "nodeType": "${permission.nodeType}",
            "nodeRef": "${permission.nodeRef}",
            "fromDate": "${permission.fromDate}",
            "userStatus": "${permission.userStatus}",
            "userLogin": "${permission.userLogin}",
            "groupName": "${permission.groupName}",
            "permissionType": "${permission.permissionType}"
        }<#if permission_has_next>,</#if>
        </#list>
    ],
    "appliedFilters": {
        "userStatus": "${appliedFilters.userStatus}",
        "fromDate": "${appliedFilters.fromDate}",
        "usernameSearch": "${appliedFilters.usernameSearch}"
    }
}
