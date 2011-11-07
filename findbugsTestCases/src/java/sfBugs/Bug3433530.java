package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3433530 {

    int x;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Bug3433530))
            return false;
        Bug3433530 other = (Bug3433530) obj;
        if (x != other.x)
            return false;
        return true;
    }

    static class Subclass extends Bug3433530 {
        static int equalsCalls = 0;

        @NoWarning("EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC")
        @Override
        public boolean equals(Object obj) {
            equalsCalls++;
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof Bug3433530))
                return false;
            Bug3433530 other = (Bug3433530) obj;
            if (x != other.x)
                return false;
            return true;
        }
    }

}
