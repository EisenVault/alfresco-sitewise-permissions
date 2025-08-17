package com.eisenvault.sitewisepermissions.platformsample;

/**
 * Utility class for permission-related operations.
 * Contains common methods used across multiple permission classes.
 */
public class PermissionUtils {
    
    /**
     * Check if this is a system permission that should be ignored
     */
    public static boolean isSystemPermission(String authority, String permission) {
        return authority.equals("GROUP_EVERYONE") || 
               authority.equals("ROLE_OWNER") || 
               authority.equals("ROLE_LOCK_OWNER") || 
               authority.equals("ROLE_VIRTUAL") ||
               authority.startsWith("ROLE_");
    }
}
