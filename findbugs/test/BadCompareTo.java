
class BadCompareTo implements Comparable<BadCompareTo> {
  int x;

  public int compareTo(BadCompareTo b) {
	return x - b.x;
	}
}
