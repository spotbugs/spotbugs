package sfBugs;

public class Bug6628576 {
	Object address, broadcast, maskLength;

	Bug6628576(Object address, Object broadcast, Object maskLength) {
		this.address = address;
		this.broadcast = broadcast;
		this.maskLength = maskLength;
	}

	public int hashCode() {
		return 42;
	}
	public boolean equals(Object obj) {
		if (!(obj instanceof Bug6628576)) {
			return false;
		}
		Bug6628576 cmp = (Bug6628576) obj;
		if ((address != null & cmp.address == null) || (!address.equals(cmp.address)))
			return false;
		if ((broadcast != null & cmp.broadcast == null) || (!broadcast.equals(cmp.broadcast)))
			return false;
		if (maskLength != cmp.maskLength)
			return false;
		return true;
	}
}
