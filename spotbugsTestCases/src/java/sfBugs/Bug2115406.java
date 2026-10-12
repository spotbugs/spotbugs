package sfBugs;

import org.jspecify.annotations.NonNull;

public class Bug2115406 {

    private final String name;

    public Bug2115406(@NonNull String name) {
        this.name = name;
    }

    public @NonNull String getName() {
        return name;
    }

    public static class ChildClass extends Bug2115406 {

        // false positive
        public static ChildClass NULL_CHILD_INSTANCE = new ChildClass(null);

        private final String childParameter;

        public ChildClass(String parameter) {
            super("foo");

            this.childParameter = parameter;
        }

        public String getChildParameter() {
            return childParameter;
        }
    }

}
