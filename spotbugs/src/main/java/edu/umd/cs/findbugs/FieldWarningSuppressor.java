package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

public class FieldWarningSuppressor extends ClassWarningSuppressor {

    private static final String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_FIELD";

    FieldAnnotation field;

    /**
     * Indicates whether this field was "user generated" as defined in {@link MemberUtils#isUserGenerated(org.apache.bcel.classfile.FieldOrMethod)}
     * When a field is not user generated we are not interested in reporting warnings, in particular we do not want to report US_USELESS_SUPPRESSION_ON_FIELD
     */
    private final boolean userGeneratedField;

    public FieldWarningSuppressor(String bugPattern,
            SuppressMatchType matchType,
            ClassAnnotation clazz,
            FieldAnnotation field,
            boolean userGeneratedClass,
            boolean userGeneratedField) {
        super(bugPattern, matchType, clazz, userGeneratedClass);
        this.field = field;
        this.userGeneratedField = userGeneratedField;
    }


    @Override
    public String toString() {
        return String.format("Suppress %s in %s.%s", bugPattern, clazz, field);
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        FieldAnnotation bugField = bugInstance.getPrimaryField();
        if (bugField == null || !field.equals(bugField)) {
            return false;
        }
        if (DEBUG) {
            System.out.println("Suppressing " + bugInstance);
        }
        return true;
    }

    @Override
    public BugInstance buildUselessSuppressionBugInstance(UselessSuppressionDetector detector) {
        return new BugInstance(detector, BUG_TYPE, PRIORITY)
                .addClass(clazz.getClassDescriptor())
                .addField(field)
                .addString(bugPattern);
    }

    @Override
    public boolean isUselessSuppressionReportable() {
        return userGeneratedField && super.isUselessSuppressionReportable();
    }
}
