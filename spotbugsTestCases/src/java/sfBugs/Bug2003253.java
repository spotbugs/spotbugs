package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bug2003253 {

    private @NonNull Object foo;

    //
    // The InconsistentAnnotations detector should report
    // a warning here.
    //
    @ExpectWarning("NP")
    public void report1(@Nullable Object bar) {
        this.foo = bar;
    }

    //
    // FindNullDeref should report a warning here.
    //
    @ExpectWarning("NP")
    public void report2(@Nullable Object bar) {
        this.foo = bar;
    }

    //
    // FindNullDeref should report a warning here.
    //
    @ExpectWarning("NP")
    public int report3(@Nullable Object bar) {
        return bar.hashCode();
    }

    //
    // FindNullDeref should report a warning here.
    //
    @ExpectWarning("NP")
    public int report4(@Nullable Object bar) {
        return nonnull(bar);
    }

    public int nonnull(@NonNull Object bar) {
        return bar.hashCode();
    }
}
