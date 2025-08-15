{
    <#if success??>
    "success": ${success?string},
    "message": "${message}"
    <#else>
    "error": "${error}"
    </#if>
}
