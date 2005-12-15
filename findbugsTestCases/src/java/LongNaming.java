class LongNaming {
	int methodf() {
		return 42;
	}

	static class A extends LongNaming {
		int methodg() {
			return 17;
		}
	}

	static class B extends A {
		int methodF() {
			return 43;
		}

		int methodG() {
			return 18;
		}

		void Wait() {
		}
	}
}
