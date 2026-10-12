package sfBugs;

import org.jspecify.annotations.NonNull;

public class Bug1924512 {

    public static @NonNull String nonNullLoop(String[] args) {
        String ret = null;
        for (String s : args) {
            ret = s;
        }
        return ret; // no warning reported here
    }

    public static @NonNull String nonNullIf(String[] args) {
        String ret = null;
        if (args.length > 0) {
            ret = args[0];
        }
        return ret; // warning correctly reported here
    }
}
