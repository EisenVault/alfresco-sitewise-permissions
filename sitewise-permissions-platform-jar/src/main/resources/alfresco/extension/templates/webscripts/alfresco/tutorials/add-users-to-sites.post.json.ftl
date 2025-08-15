{
    <#if message??>
    "message": "${message}",
    "totalAssignments": ${totalAssignments},
    "addedCount": ${addedCount},
    "errorCount": ${errorCount},
    "results": [
        <#list results as result>
        {
            "user": "${result.user}",
            "site": "${result.site}",
            <#if result.role??>"role": "${result.role}",</#if>
            "status": "${result.status}",
            "message": "${result.message}"
        }<#if result_has_next>,</#if>
        </#list>
    ]
    <#else>
    "error": "${error}"
    </#if>
}
