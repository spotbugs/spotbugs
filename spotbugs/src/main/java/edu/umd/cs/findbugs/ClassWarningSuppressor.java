package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class ClassWarningSuppressor extends WarningSuppressor {

    private static final String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_CLASS";

    ClassAnnotation clazz;

    /**
     * Indicates whether this class was "user generated" as defined in {@link MemberUtils#isUserGenerated(edu.umd.cs.findbugs.ba.XClass)}
     * When a class is not user generated we are not interested in reporting warnings, in particular we do not want to report US_USELESS_SUPPRESSION_ON_CLASS
     */
    private final boolean userGeneratedClass;

    /**
     * @param userGeneratedClass
     */
    public ClassWarningSuppressor(String bugPattern, SuppressMatchType matchType, ClassAnnotation clazz, boolean userGeneratedClass) {
        super(bugPattern, matchType);
        this.clazz = clazz;
        this.userGeneratedClass = userGeneratedClass;

        if (DEBUG) {
            System.out.println("Suppressing " + bugPattern + " in " + clazz);
        }
    }

    public ClassAnnotation getClassAnnotation() {
        return clazz;
    }

    @Override
    public BugInstance buildUselessSuppressionBugInstance(UselessSuppressionDetector detector) {
        return new BugInstance(detector, BUG_TYPE, PRIORITY)
                .addClass(clazz.getClassDescriptor())
                .addString(adjustBugPatternForMessage());
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        MethodAnnotation bugMethod = bugInstance.getPrimaryMethod();
        if (bugPattern == null && bugMethod != null) {
            try {
                JavaClass javaClass = Repository.lookupClass(bugMethod.getClassName());
                Method method = null;
                for (Method candidate : javaClass.getMethods()) {
                    if (bugMethod.getMethodName().equals(candidate.getName())
                            && bugMethod.getMethodSignature().equals(candidate.getSignature())) {
                        method = candidate;
                        break;
                    }
                }
                if (method == null || MemberUtils.isUserGenerated(method)) {
                    return false;
                }
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
        if (DEBUG) {
            System.out.println("Compare " + primaryClassAnnotation + " with " + clazz);
        }

        return clazz.contains(primaryClassAnnotation);

    }

    @Override
    public boolean isUselessSuppressionReportable() {
        return userGeneratedClass;
    }
}
