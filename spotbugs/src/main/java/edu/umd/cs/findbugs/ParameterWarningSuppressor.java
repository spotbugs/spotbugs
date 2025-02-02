package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;

public class ParameterWarningSuppressor extends ClassWarningSuppressor {

    final MethodAnnotation method;

    final int register;

    public ParameterWarningSuppressor(String bugPattern, SuppressMatchType matchType, ClassAnnotation clazz, MethodAnnotation method, int register) {
        super(bugPattern, matchType, clazz);
        this.method = method;
        this.register = register;
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        MethodAnnotation bugMethod = bugInstance.getPrimaryMethod();
        LocalVariableAnnotation localVariable = bugInstance.getPrimaryLocalVariableAnnotation();
        if (bugMethod == null || !method.equals(bugMethod)) {
            return false;
        }
        if (localVariable == null || localVariable.getRegister() != register) {
            return false;
        }
        if (DEBUG) {
            System.out.println("Suppressing " + bugInstance);
        }
        return true;
    }
}
