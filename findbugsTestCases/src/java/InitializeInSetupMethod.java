
public class InitializeInSetupMethod {
	String x;
	@Override
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
