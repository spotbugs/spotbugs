package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3056289 {
    private static Bug3056289 instance;

    @NoWarning("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR,ST_WRITE_TO_STATIC_FROM_INSTANCE")
    public Bug3056289() {
        if (instance != null) {
            throw new IllegalStateException("I'm a singleton!");
        }
        instance = this; // ST_WRITE_TO_STATIC_FROM_INSTANCE is reported here!
    }
}
