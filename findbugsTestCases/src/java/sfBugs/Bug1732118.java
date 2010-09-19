package sfBugs;

public class Bug1732118 {
    Object filter;

    public Object getFilter() {
        return filter;
    }

    public void setFilter(Object filter) {
        this.filter = filter;
    }

    public synchronized int getHash() {
        if (filter != null)
            return filter.hashCode();
        return 0;
    }
}
