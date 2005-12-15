import edu.umd.cs.findbugs.annotations.*;

class NeedsReturnValueChecked {

	@CheckReturnValue
	int foo() {
		return 42;
	}
}
