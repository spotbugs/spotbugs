package equals;

public class EqualToAString {
	
	final String name;
	
	public EqualToAString(String name) {
		this.name = name;
	}
	
	public boolean equals(Object o) {
		if (o instanceof EqualToAString) 
			return name.equals(((EqualToAString)o).name);
		if (o instanceof String) {
			return name.equals((String) o);
		}
		return false;
	}

}
