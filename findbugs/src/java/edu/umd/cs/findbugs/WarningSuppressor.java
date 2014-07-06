package edu.umd.cs.findbugs;

import java.io.IOException;

import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.xml.XMLOutput;

abstract public class WarningSuppressor implements Matcher {

    final static boolean DEBUG = SystemProperties.getBoolean("warning.suppressor");

    String bugPattern;

    public WarningSuppressor(String bugPattern) {
        this.bugPattern = bugPattern;
        if (DEBUG) {
            System.out.println("Suppressing " + bugPattern);
        }
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (DEBUG) {
            System.out.println("Checking " + bugInstance);
            System.out.println("    type:" + bugInstance.getType());
            System.out.println(" against: " + bugPattern);

        }
        if (!(bugPattern == null || bugInstance.getType().startsWith(bugPattern)
                || bugInstance.getBugPattern().getCategory().equalsIgnoreCase(bugPattern) || bugInstance.getBugPattern()
                .getAbbrev().equalsIgnoreCase(bugPattern))) {
            return false;
        }
        if (DEBUG) {
            System.out.println(" pattern matches");
        }
        return true;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        // no-op; these aren't saved to XML
    }
}
