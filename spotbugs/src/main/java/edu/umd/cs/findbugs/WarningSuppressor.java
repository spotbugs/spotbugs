package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.detect.NoteSuppressedWarnings;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.xml.XMLOutput;

public abstract class WarningSuppressor implements Matcher {

    protected static final String USELESS_SUPPRESSION_ABB = "US";
    protected static final int PRIORITY = Priorities.NORMAL_PRIORITY;

    static final boolean DEBUG = SystemProperties.getBoolean("warning.suppressor");

    protected final String bugPattern;
    protected final SuppressMatchType matchType;

    private Collection<WarningSuppressor> alternateSuppressors = Collections.emptySet();

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
        if (USELESS_SUPPRESSION_ABB.equals(bugInstance.getAbbrev())) {
            return false;
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

    /**
     * @return true if useless suppressions should be reported.
     */
    public boolean isUselessSuppressionReportable() {
        return true;
    }

    public abstract BugInstance buildUselessSuppressionBugInstance(UselessSuppressionDetector detector);

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        // no-op; these aren't saved to XML
    }

    public void addAlternateSuppressors(Collection<WarningSuppressor> additionalSuppressors) {
        if (alternateSuppressors.isEmpty()) {
            alternateSuppressors = new ArrayList<>(additionalSuppressors);
        } else {
            alternateSuppressors.addAll(additionalSuppressors);
        }
    }

    /**
     * @return The alternate suppressors that might have been generated from a single <code>SuppressWarnings</code> annotation.
     * See {@link NoteSuppressedWarnings}
     */
    public Collection<WarningSuppressor> getAlternateSuppressors() {
        return alternateSuppressors;
    }
}
