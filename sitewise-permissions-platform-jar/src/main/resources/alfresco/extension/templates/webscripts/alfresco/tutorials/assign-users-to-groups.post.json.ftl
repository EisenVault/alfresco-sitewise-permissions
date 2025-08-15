{
    <#if message??>
    "message": "${message}",
    "totalAssignments": ${totalAssignments},
    "assignedCount": ${assignedCount},
    "errorCount": ${errorCount},
    "results": [
        <#list results as result>
        {
            "user": "${result.user}",
            "group": "${result.group}",
            "status": "${result.status}",
            "message": "${result.message}"
        }<#if result_has_next>,</#if>
        </#list>
    ]
    <#else>
    "error": "${error}"
    </#if>
}
