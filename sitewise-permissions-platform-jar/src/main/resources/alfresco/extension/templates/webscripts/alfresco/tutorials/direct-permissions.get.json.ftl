{
    "success": ${(success!false)?string},
    <#if error??>
    "error": "${error}"
    <#else>
    "site": "${site!""}",
    "totalNodes": ${totalNodes!0},
    "totalPermissions": ${totalPermissions!0},
    "userPermissions": ${userPermissions!0},
    "groupPermissions": ${groupPermissions!0},
    "effectivePermissions": ${effectivePermissions!0},
    "permissions": [
        <#if permissions??>
        <#list permissions as permission>
        {
            "username": "${permission.username!""}",
            "site": "${permission.site!""}",
            "nodePath": "${permission.nodePath!""}",
            "role": "${permission.role!""}",
            "nodeName": "${permission.nodeName!""}",
            "nodeType": "${permission.nodeType!""}",
            "nodeRef": "${permission.nodeRef!""}",
            "fromDate": "${permission.fromDate!""}",
            "userStatus": "${permission.userStatus!""}",
            "userLogin": "${permission.userLogin!""}",
            "groupName": "${permission.groupName!""}",
            "permissionType": "${permission.permissionType!""}"
        }<#if permission_has_next>,</#if>
        </#list>
        </#if>
    ]
    </#if>
}
