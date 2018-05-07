package ghIssues;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public enum Issue616 {
    TEST("one", "two", null);

    @NonNull
    public final String one;
    @NonNull
    public final String two;
    @Nullable
    public final String three;

    private Issue616(@NonNull String one, @NonNull String two, @Nullable Object three) {
        this.one = Preconditions.checkNotNull(one);
        this.two = Preconditions.checkNotNull(two);
        this.three = three == null ? null : three.toString();
    }
}
