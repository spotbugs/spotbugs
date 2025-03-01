package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;

public class PackageWarningSuppressor extends WarningSuppressor {

    private static final String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_PACKAGE";

    String packageName;

    public PackageWarningSuppressor(String bugPattern, SuppressMatchType matchType, String packageName) {
        super(bugPattern, matchType);
        this.packageName = packageName;
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
    public BugInstance buildUselessSuppressionBugInstance() {
        return new BugInstance(BUG_TYPE, PRIORITY).addClass(packageName);
    }
}
