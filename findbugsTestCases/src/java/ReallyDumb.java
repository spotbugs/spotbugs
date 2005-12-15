import java.util.*;

class ReallyDumb implements Iterator {

	public boolean hasNext() {
		return next() != null;
	}

	public Object next() {
		return "1".substring(0);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
