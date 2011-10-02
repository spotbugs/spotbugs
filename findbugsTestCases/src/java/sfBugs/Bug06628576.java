package sfBugs;

public class Bug06628576 {
    Object address, broadcast, maskLength;

    Bug06628576(Object address, Object broadcast, Object maskLength) {
        this.address = address;
        this.broadcast = broadcast;
        this.maskLength = maskLength;
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bug06628576)) {
            return false;
        }
        Bug06628576 cmp = (Bug06628576) obj;
        if ((address != null & cmp.address == null) || (!address.equals(cmp.address)))
            return false;
        if ((broadcast != null & cmp.broadcast == null) || (!broadcast.equals(cmp.broadcast)))
            return false;
        if (maskLength != cmp.maskLength)
            return false;
        return true;
    }
}
