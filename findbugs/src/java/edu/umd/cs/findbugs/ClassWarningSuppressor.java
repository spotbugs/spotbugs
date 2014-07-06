package edu.umd.cs.findbugs;

public class ClassWarningSuppressor extends WarningSuppressor {

    ClassAnnotation clazz;

    public ClassWarningSuppressor(String bugPattern, ClassAnnotation clazz) {
        super(bugPattern);
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
