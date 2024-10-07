package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;

public class ClassWarningSuppressor extends WarningSuppressor {

    ClassAnnotation clazz;

    public ClassWarningSuppressor(String bugPattern, SuppressMatchType matchType, ClassAnnotation clazz) {
        super(bugPattern, matchType);
        this.clazz = clazz;
        if (DEBUG) {
            System.out.println("Suppressing " + bugPattern + " in " + clazz);
        }
    }

    public ClassAnnotation getClassAnnotation() {
        return clazz;
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
        if (DEBUG) {
            System.out.println("Compare " + primaryClassAnnotation + " with " + clazz);
        }

        return clazz.contains(primaryClassAnnotation);

    }
}
