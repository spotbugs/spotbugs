import java.util.*;

class DumbMethodInvocations implements Iterator {

	String f(String s) {
		return s.substring(0);
	}

	String g(String s) {
		for (int i = 0; i < s.length(); i++)
			if (s.substring(i).hashCode() == 42)
				return s;
		return null;
	}

	public boolean hasNext() {
		return next() != null;
	}

	public Object next() {
		return null;
	}

	public void remove() {
	}

}
