abstract class SubclassHashCodeEquals {
	int x;

	@Override
	public int hashCode() {
		return x;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SubclassHashCodeEquals))
			return false;
		return x == ((SubclassHashCodeEquals) o).x;
	}

	static class Y extends SubclassHashCodeEquals {
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Y))
				return false;
			return super.equals(o);
		}
	}
}
