package sfBugs;

public class Bug6628576a {
    Object address;

    Bug6628576a(Object address) {
        this.address = address;

    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bug6628576a))
            return false;

        Bug6628576a cmp = (Bug6628576a) obj;
        if ((address != null & cmp.address == null) || (!address.equals(cmp.address)))
            return false;
        return true;
    }
}
