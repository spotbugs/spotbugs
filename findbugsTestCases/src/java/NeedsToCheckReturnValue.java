import edu.umd.cs.findbugs.annotations.*;

class NeedsToCheckReturnValue {

	int bar(NeedsReturnValueChecked n) {
		n.foo();
		return 42;
	}
}
