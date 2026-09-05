package ghIssues;

public class Issue3463 {
	private static int staticCounter;
	
	private int counter;
	
	public int testField() {
		// Expecting ASE_ASSERTION_WITH_SIDE_EFFECT here
		assert ++counter > 0;
		return counter;
	}
	
	public static int testStaticField() {
		// Expecting ASE_ASSERTION_WITH_SIDE_EFFECT here
		assert ++staticCounter > 0;
		return staticCounter;
	}
}
