package ghIssues;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.Nullable;

public enum Issue616 {
    TEST("one", "two", null);

    public final String one;
    public final String two;
    @Nullable
    public final String three;

    private Issue616(String one, String two, @Nullable Object three) {
        this.one = Preconditions.checkNotNull(one);
        this.two = Preconditions.checkNotNull(two);
        this.three = three == null ? null : three.toString();
    }
}
