{
    "success": ${(success!false)?string},
    <#if error??>
    "error": "${error}"
    <#else>
    "statusFilter": "${statusFilter!""}",
    "totalUsers": ${totalUsers!0},
    "users": [
        <#if users??>
        <#list users as user>
        {
            "username": "${user.username!""}",
            "status": "${user.status!""}",
            "lastLogin": "${user.lastLogin!""}",
            "firstName": "${user.firstName!""}",
            "lastName": "${user.lastName!""}",
            "email": "${user.email!""}"
        }<#if user_has_next>,</#if>
        </#list>
        </#if>
    ]
    </#if>
}
