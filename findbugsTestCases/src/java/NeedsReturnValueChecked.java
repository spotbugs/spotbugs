import edu.umd.cs.findbugs.annotations.CheckReturnValue;

class NeedsReturnValueChecked {

	@CheckReturnValue
	int foo() {
		return 42;
	}
}
