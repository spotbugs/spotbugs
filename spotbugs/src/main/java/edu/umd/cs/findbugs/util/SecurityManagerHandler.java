package edu.umd.cs.findbugs.util;

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Since Java 17, the security manager is deprecated for removal and invoking related methods
 * causes a warning to be printed to the console. This intermediate disables the use of
 * security manager-related APIs on Java 17 or later, unless using the security manager is
 * explicitly configured by setting the <i>edu.umd.cs.findbugs.securityManagerDisabled</i>
 * property.
 */
public class SecurityManagerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityManagerHandler.class);

    /**
     * Determines if the security manager is used by SpotBugs.
     */
    private static final boolean SECURITY_MANAGER_DISABLED;

    static {
        boolean securityManagerDisabled;
        try {
            String property = System.getProperty("edu.umd.cs.findbugs.securityManagerDisabled");
            if (property != null) {
                securityManagerDisabled = Boolean.parseBoolean(property);
            } else {
                securityManagerDisabled = SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_17);
            }
        } catch (Throwable t) {
            securityManagerDisabled = false;
            LOGGER.debug("failed to detect the ability of security manager feature, so treat it as available", t);
        }
        SECURITY_MANAGER_DISABLED = securityManagerDisabled;
    }

    /**
     * Disables the security manager by setting {@link System#setSecurityManager(SecurityManager)}
     * to {@code null}.
     */
    public static void disableSecurityManager() {
        if (SECURITY_MANAGER_DISABLED) {
            return;
        }
        doDisableSecurityManager();
    }

    /**
     * This method is a safeguard for running this library on a JVM that might no longer include
     * the security manager API after removal. As the JVM verifies methods lazily, and since this
     * method will never be invoked, validation of this method with a missing type can never fail.
     *
     * As some environments do not support setting the security manager but always return null
     * when getting it, we check if a security manager is set before disabling it.
     */
    private static void doDisableSecurityManager() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            System.setSecurityManager(null);
        }
    }
}
