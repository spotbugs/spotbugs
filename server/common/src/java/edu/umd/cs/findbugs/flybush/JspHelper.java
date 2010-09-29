package edu.umd.cs.findbugs.flybush;

public final class JspHelper {
    private static final String CLOUD_NAME_SYSPROP = System.getProperty("findbugs.cloud.name");

    public String getCloudName() {
        String name = CLOUD_NAME_SYSPROP;
        if (name == null || name.trim().length() == 0) {
            name = "FindBugs Cloud";
        }
        return name.trim();
    }
}
