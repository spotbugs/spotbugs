
class NeedsToCheckReturnValue {

	int bar(NeedsReturnValueChecked n) {
		n.foo();
		return 42;
	}
}
