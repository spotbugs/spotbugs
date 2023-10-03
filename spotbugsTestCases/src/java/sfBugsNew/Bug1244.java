package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1244 {
    private final int a;

    public Bug1244(int a) {
        this.a = a;
    }

    @Override
	public int hashCode() {
        return a;
    }

    @Override
    @NoWarning("BC_EQUALS_METHOD_SHOULD_WORK_FOR_ALL_OBJECTS")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!getClass().isInstance(obj))
            return false;
        Bug1244 other = (Bug1244) obj;
        if (a != other.a)
            return false;
        return true;
    }

   static public class Wrong {
        private final int a;

        public Wrong(int a) {
            this.a = a;
        }

        @Override
		public int hashCode() {
            return a;
        }

        @Override
        @ExpectWarning("BC_EQUALS_METHOD_SHOULD_WORK_FOR_ALL_OBJECTS")
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            Wrong other = (Wrong) obj;
            if (a != other.a)
                return false;
            return true;
        }
   }
}
