abstract class AbstractMissingHashCode {
	int x;

	public boolean equals(Object o) {
		if (!(o instanceof AbstractMissingHashCode))
			return false;
		return x == ((AbstractMissingHashCode) o).x;
	}

	static class StillMissingHashCode extends AbstractMissingHashCode {
		int y;
	}
}
