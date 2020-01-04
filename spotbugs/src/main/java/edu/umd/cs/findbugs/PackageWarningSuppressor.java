package edu.umd.cs.findbugs;

public class PackageWarningSuppressor extends WarningSuppressor {

    private final static String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_PACKAGE";

    String packageName;

    public PackageWarningSuppressor(String bugPattern, String packageName) {
        super(bugPattern);
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
