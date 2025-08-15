{
    "message": "Group creation completed",
    "totalGroups": ${totalGroups},
    "createdCount": ${createdCount},
    "existingCount": ${existingCount},
    "results": [
        <#list results as result>
        {
            "groupName": "${result.groupName}",
            "status": "${result.status}",
            "message": "${result.message}"
        }<#if result_has_next>,</#if>
        </#list>
    ]
}
