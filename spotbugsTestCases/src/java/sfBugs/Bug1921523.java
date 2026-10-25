package sfBugs;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bug1921523 {
    public @Nullable Integer nullablePerhaps(int x) {
        return x % 2 == 0 ? null : x;
    }

    public @Nullable Integer nullableAlways(int x) {
        return null;
    }

    public @Nullable Integer nullableNever(int x) {
        return 7;
    }

    public @NonNull Integer nonNullIndirectPerhaps(int x) {
        return nullablePerhaps(x);
    }

    public @NonNull Integer nonNullIndirectAlways(int x) {
        return nullableAlways(x);
    }

    public @NonNull Integer nonNullIndirectNever(int x) {
        return nullableNever(x);
    }

    public @NonNull Integer nonNullPerhaps(int x) {
        return (x % 2 == 0 ? null : x);
    }

    public @NonNull Integer nonNullAlways(int x) {
        return null;
    }

    public String deadGiveavay(int x) {
        return (x % 2 == 0 ? null : x).toString();
    }
}
