package npe;

// From java.net.InterfaceAddress
public class InterfaceAddress {
	String address, broadcast;
	int maskLength;
	public InterfaceAddress( String address, String broadcast, int maskLength) {
		this.address = address;
		this.broadcast = broadcast;
		this.maskLength = maskLength;
	}
	public int hashCode() {
		int result = 0;
		if (address != null) result ^= address.hashCode();
		if (broadcast != null) result ^= broadcast.hashCode();
		result ^= maskLength;
		return result;
	}
    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress cmp = (InterfaceAddress) obj;
        if ((address != null & cmp.address == null) ||
            (!address.equals(cmp.address)))
            return false;
        if ((broadcast != null & cmp.broadcast == null) ||
            (!broadcast.equals(cmp.broadcast)))
            return false;
        if (maskLength != cmp.maskLength)
            return false;
        return true;
    }


}
