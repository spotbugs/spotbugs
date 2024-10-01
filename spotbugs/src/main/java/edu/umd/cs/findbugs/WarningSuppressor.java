package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.xml.XMLOutput;

abstract public class WarningSuppressor implements Matcher {

    final static boolean DEBUG = SystemProperties.getBoolean("warning.suppressor");

    protected final String bugPattern;
    protected final SuppressMatchType matchType;

    protected WarningSuppressor(String bugPattern, SuppressMatchType matchType) {
        this.bugPattern = bugPattern;

        if (matchType == null) {
            this.matchType = SuppressMatchType.DEFAULT;
        } else {
            this.matchType = matchType;
        }

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

        if (bugPattern != null) {
            switch (matchType) {
            case DEFAULT:
                if (!bugInstance.getType().startsWith(bugPattern)
                        && !bugInstance.getBugPattern().getCategory().equalsIgnoreCase(bugPattern)
                        && !bugInstance.getBugPattern().getAbbrev().equalsIgnoreCase(bugPattern)) {
                    return false;
                }
                break;
            case EXACT:
                if (!bugInstance.getType().equals(bugPattern)
                        && !bugInstance.getBugPattern().getCategory().equals(bugPattern)
                        && !bugInstance.getBugPattern().getAbbrev().equals(bugPattern)) {
                    return false;
                }
                break;
            case REGEX:
                Pattern pattern = Pattern.compile(bugPattern);
                if (!pattern.matcher(bugInstance.getType()).matches()
                        && !pattern.matcher(bugInstance.getBugPattern().getCategory()).matches()
                        && !pattern.matcher(bugInstance.getBugPattern().getAbbrev()).matches()) {
                    return false;
                }
                break;
            default:
                break;
            }
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
