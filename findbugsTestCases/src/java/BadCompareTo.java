public class BadCompareTo implements Comparable<BadCompareTo> {
	int x;

	public int compareTo(BadCompareTo b) {
		return x - b.x;
	}
}

class GoodCompareTo {
	int x;

	public int compareTo(GoodCompareTo g) {
		return x - g.x;
	}
}