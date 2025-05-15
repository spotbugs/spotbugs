package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

public class PackageWarningSuppressor extends WarningSuppressor {

    private static final String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_PACKAGE";

    String packageName;

    /**
     * Indicates whether this package was "user generated" as defined in {@link MemberUtils#isUserGenerated(edu.umd.cs.findbugs.ba.XClass)}
     * When a package is not user generated we are not interested in reporting warnings, in particular we do not want to report US_USELESS_SUPPRESSION_ON_PACKAGE
     */
    private final boolean userGeneratedPackage;

    public PackageWarningSuppressor(String bugPattern, SuppressMatchType matchType, String packageName, boolean userGeneratedPackage) {
        super(bugPattern, matchType);
        this.packageName = packageName;
        this.userGeneratedPackage = userGeneratedPackage;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
        if (DEBUG) {
            System.out.println("Compare " + primaryClassAnnotation + " with " + packageName);
        }

        String className = primaryClassAnnotation.getClassName();

        return className.startsWith(packageName);
    }

    @Override
    public BugInstance buildUselessSuppressionBugInstance(UselessSuppressionDetector detector) {
        return new BugInstance(detector, BUG_TYPE, PRIORITY)
                .addClass(packageName)
                .addString(bugPattern);
    }

    @Override
    public boolean isUselessSuppressionReportable() {
        return userGeneratedPackage;
    }
}
