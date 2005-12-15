class MethodCapitalization {
	public void MethodCapitalization() {
	}

	public int getX() {
		return 42;
	}

	public int hashcode() {
		return 42;
	}

	static class Foo extends MethodCapitalization {
		public int getx() {
			return 42;
		}
	}

	static class Bar {
		public int getx() {
			return 42;
		}
	}
}
