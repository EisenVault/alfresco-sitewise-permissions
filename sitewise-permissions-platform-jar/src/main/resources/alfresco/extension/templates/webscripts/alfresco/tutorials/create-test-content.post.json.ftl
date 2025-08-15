{
    <#if message??>
    "message": "${message}",
    "totalFolders": ${totalFolders},
    "createdCount": ${createdCount},
    "errorCount": ${errorCount},
    "results": [
        <#list results as result>
        {
            "site": "${result.site}",
            "folder": "${result.folder}",
            "status": "${result.status}",
            "message": "${result.message}"
        }<#if result_has_next>,</#if>
        </#list>
    ]
    <#else>
    "error": "${error}"
    </#if>
}
