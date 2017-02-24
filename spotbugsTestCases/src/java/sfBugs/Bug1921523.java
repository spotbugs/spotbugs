package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug1921523 {
    public @CheckForNull
    Integer nullablePerhaps(int x) {
        return x % 2 == 0 ? null : x;
    }

    public @CheckForNull
    Integer nullableAlways(int x) {
        return null;
    }

    public @CheckForNull
    Integer nullableNever(int x) {
        return 7;
    }

    public @NonNull
    Integer nonNullIndirectPerhaps(int x) {
        return nullablePerhaps(x);
    }

    public @NonNull
    Integer nonNullIndirectAlways(int x) {
        return nullableAlways(x);
    }

    public @NonNull
    Integer nonNullIndirectNever(int x) {
        return nullableNever(x);
    }

    public @NonNull
    Integer nonNullPerhaps(int x) {
        return (x % 2 == 0 ? null : x);
    }

    public @NonNull
    Integer nonNullAlways(int x) {
        return null;
    }

    public String deadGiveavay(int x) {
        return (x % 2 == 0 ? null : x).toString();
    }
}
