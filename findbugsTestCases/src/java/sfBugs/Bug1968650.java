package sfBugs;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug1968650 {
    public void nonNullArgsMethod(@NonNull Object any) {

    }

    @NonNull
    public Object nonNullReturnValue() {
        nonNullArgsMethod(null);
        return null;
    }
}
