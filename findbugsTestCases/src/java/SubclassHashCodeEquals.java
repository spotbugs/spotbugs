abstract class SubclassHashCodeEquals {
	int x;

	public int hashCode() {
		return x;
	}

	public boolean equals(Object o) {
		if (!(o instanceof SubclassHashCodeEquals))
			return false;
		return x == ((SubclassHashCodeEquals) o).x;
	}

	static class Y extends SubclassHashCodeEquals {
		public boolean equals(Object o) {
			if (!(o instanceof Y))
				return false;
			return super.equals(o);
		}
	}
}
