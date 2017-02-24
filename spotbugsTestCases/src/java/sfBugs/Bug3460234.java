package sfBugs;

public class Bug3460234 {
    public static class Base {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", justification = "Test code")
        public boolean equals(Object other) {
            if (other instanceof Base) // Marker for the problem shows up here
                                       // in Eclipse.
                return (this == other);

            return (false);
        }

        @Override
        public int hashCode() {
            return (42);
        }
    }

    public static class Middle extends Base {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", justification = "Test code")
        public boolean equals(Object other) {
            if (other instanceof Middle)
                return (this == other);

            return (false);
        }
    }

    public static class Many extends Middle {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", justification = "Test code")
        public boolean equals(Object other) {
            if (other instanceof Many)
                return (this == other);

            return (false);
        }
    }

}
