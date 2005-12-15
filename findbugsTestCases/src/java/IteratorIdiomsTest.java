//IteratorIdioms examples

import java.util.Iterator;

public class IteratorIdiomsTest implements Iterator {
	public Object next() {
		return null;
	}

	public boolean hasNext() {
		return false;
	}

	public void remove() {
	}
}

class NotAnIterator {
	public Object next() {
		return null;
	}

	public boolean hasNext() {
		return false;
	}

	public void remove() {
	}
}