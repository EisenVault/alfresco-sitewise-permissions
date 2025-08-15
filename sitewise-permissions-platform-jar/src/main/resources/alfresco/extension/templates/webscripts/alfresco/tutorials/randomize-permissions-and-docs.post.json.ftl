{
    "success": ${(success!false)?string},
    <#if error??>
    "error": "${error}"
    <#else>
    "site": "${site!""}",
    "foldersProcessed": ${foldersProcessed!0},
    "docsCreated": ${docsCreated!0},
    "permissionsApplied": ${permissionsApplied!0},
    "errors": ${errors!0},
    "results": [
        <#if results??>
        <#list results as result>
        {
            <#list result?keys as key>
            "${key}": "${result[key]!""}"<#if key_has_next>,</#if>
            </#list>
        }<#if result_has_next>,</#if>
        </#list>
        </#if>
    ]
    </#if>
}
