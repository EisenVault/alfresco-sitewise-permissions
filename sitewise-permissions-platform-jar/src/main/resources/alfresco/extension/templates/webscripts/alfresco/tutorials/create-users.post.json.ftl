{
    "message": "User creation completed",
    "totalUsers": ${totalUsers},
    "createdCount": ${createdCount},
    "existingCount": ${existingCount},
    "results": [
        <#list results as result>
        {
            "username": "${result.username}",
            "status": "${result.status}",
            "message": "${result.message}"
        }<#if result_has_next>,</#if>
        </#list>
    ]
}
