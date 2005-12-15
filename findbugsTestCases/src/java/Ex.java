public class Ex {

	public void frotz(Object o, int y) {
		if (y == 6) {
			synchronized (o) {
				y += o.hashCode();
				if ((y & 1) == 0)
					throw new RuntimeException();
			}
		}
	}

	public void alwaysThrow() {
		throw new RuntimeException();
	}

}
