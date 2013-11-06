package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug3258757 {
    public static void main(String[] args) {
        final String str = getString();

        if(System.currentTimeMillis() > 0) {
            // this does not complain
            useString(str);
        }

        // this complains:
        // M D NP: Possible null pointer dereference in Demo.main(String[]) due to return value of called method  Method invoked at Demo.java:[line 14]
        useString(str);
    }

    public static void other1(@CheckForNull String str) {
        if(System.currentTimeMillis() > 0) {
            // this does not complain
            useString(str);
        }
    }

    // this complains:
    // H D NP: $L0 must be nonnull but is marked as nullable  At Demo.java:[lines 27-28]
    public static void other2(@CheckForNull String str) {
        useString(str);
    }

    @CheckForNull
    public static String getString() {
        return null;
    }

    private static void useString(@NonNull String str) {
    }
}

