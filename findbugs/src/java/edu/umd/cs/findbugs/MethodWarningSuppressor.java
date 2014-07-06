package edu.umd.cs.findbugs;

public class MethodWarningSuppressor extends ClassWarningSuppressor {

    MethodAnnotation method;

    public MethodWarningSuppressor(String bugPattern, ClassAnnotation clazz, MethodAnnotation method) {
        super(bugPattern, clazz);
        this.method = method;
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        MethodAnnotation bugMethod = bugInstance.getPrimaryMethod();
        if (bugMethod != null && !method.equals(bugMethod)) {
            return false;
        }
        if (DEBUG) {
            System.out.println("Suppressing " + bugInstance);
        }
        return true;
    }
}
