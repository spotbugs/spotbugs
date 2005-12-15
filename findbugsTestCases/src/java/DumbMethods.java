import java.util.Collection;

class DumbMethods {

	static public String getStringOfString(String s) {
		return s.toString();
	}

	static public boolean isCollection(Object o) {
		return ((o != null) && (o instanceof Collection));
	}
}
