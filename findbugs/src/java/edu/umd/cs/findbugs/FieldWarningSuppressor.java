
package edu.umd.cs.findbugs;

public class FieldWarningSuppressor extends ClassWarningSuppressor {

	FieldAnnotation field;


	public FieldWarningSuppressor(String bugPattern, ClassAnnotation clazz, FieldAnnotation field) {
		super(bugPattern, clazz);
		this.field = field;
		}
	@Override
	public boolean match(BugInstance bugInstance) {

		if (!super.match(bugInstance)) return false;

	FieldAnnotation bugField = bugInstance.getPrimaryField();
	if (bugField == null ||
		!field.equals(bugField)) return false;
	if (DEBUG)
	System.out.println("Suppressing " + bugInstance);
	return true;
	}
}

