package sfBugs;

public class Bug3431227<T extends Bug3431227<T>> implements Comparable<T> {
    private int _i;

    public Bug3431227(int i) {
        _i = i;
    }

    public int getI() {
        return _i;
    }

    public int compareTo(T other) {
        return other.getI() - getI();
    }

    
    @Override
    public int hashCode() {
        return _i;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return _i == ((Bug3431227) obj)._i;
    }
    
    static class Normal extends Bug3431227<Normal> {
        public Normal(int i) {
            super(i);
        }

        @Override
        public int compareTo(Normal other) {
            return getI() - other.getI();
        }
    }

}

