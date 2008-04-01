package edu.umd.cs.findbugs;

public class ParameterWarningSuppressor extends ClassWarningSuppressor {

	final MethodAnnotation method;

	final int register;

	public ParameterWarningSuppressor(String bugPattern, ClassAnnotation clazz, MethodAnnotation method, int register) {
		super(bugPattern, clazz);
		this.method = method;
		this.register = register;
	}

	@Override
	public boolean match(BugInstance bugInstance) {

		if (!super.match(bugInstance))
			return false;

		MethodAnnotation bugMethod = bugInstance.getPrimaryMethod();
		LocalVariableAnnotation localVariable = bugInstance.getPrimaryLocalVariableAnnotation();
		if (bugMethod == null || !method.equals(bugMethod))
			return false;
		if (localVariable == null || localVariable.getRegister() != register)
			return false;
		if (DEBUG)
			System.out.println("Suppressing " + bugInstance);
		return true;
	}
}
