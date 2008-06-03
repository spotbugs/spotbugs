package sfBugs;

public class Bug1983674a {
	Object address;

	Bug1983674a(Object address) {
		this.address = address;
		
	}

	public int hashCode() {
		return 42;
	}
	public boolean equals(Object obj) {
		if (!(obj instanceof Bug1983674a)) 
			return false;
		
		Bug1983674a cmp = (Bug1983674a) obj;
		if ((address != null & cmp.address == null) || (!address.equals(cmp.address)))
			return false;
		return true;
	}
}
