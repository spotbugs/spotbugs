
public class InitializeInSetupMethod {
	String x;
	public int hashCode() {
		return x.hashCode();
	}
	InitializeInSetupMethod() {
		setUp();
	}
	private void setUp() {
		x = "foo";
	}
}
