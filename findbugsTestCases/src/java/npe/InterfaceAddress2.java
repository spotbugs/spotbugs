package npe;

// From java.net.InterfaceAddress
public class InterfaceAddress2 {
	String address, broadcast;
	int maskLength;
	public InterfaceAddress2( String address, String broadcast, int maskLength) {
		this.address = address;
		this.broadcast = broadcast;
		this.maskLength = maskLength;
	}
	@Override
	public int hashCode() {
		int result = 0;
		if (address != null) result ^= address.hashCode();
		if (broadcast != null) result ^= broadcast.hashCode();
		result ^= maskLength;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InterfaceAddress2)) {
            return false;
		}
		InterfaceAddress2 cmp = (InterfaceAddress2) obj;
		if ((address != null && cmp.address == null) ||
            (!address.equals(cmp.address)))
			return false;
		if ((broadcast != null && cmp.broadcast == null) ||
			(!broadcast.equals(cmp.broadcast)))
            return false;
		if (maskLength != cmp.maskLength)
			return false;
		return true;
    }


}
