package ghIssues;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * False negatives and positives of NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE.
 * Results surprisingly depends on the actual formatting of the source code.
 */
public class Issue782 {

    /**
     * Invoking get() which is annotated with a custom Nullable.
     */
    public void func() {
        Object a = get(); // true positive
        Object b = checkNotNull(get()); // true negative
        Object c = checkNotNull(
                get()); // false positive (fixed by the change)
        System.out.println(a.toString());
        System.out.println(b.toString());
        System.out.println(c.toString());
    }

    /**
     * Invoking get2() which is annotated with a javax Nullable, verify return value with Guava checkNotNull.
     */
    public void func2() {
        Object a = get2(); // @Nullable is @Nonnull(when = When.UNKNOWN) so it seems OK that we're not reporting a bug
        Object b = checkNotNull(get2()); // true negative
        Object c = checkNotNull(
                get2()); // true negative
        System.out.println(a.toString());
        System.out.println(b.toString());
        System.out.println(c.toString());
    }

    /**
     * Invoking get2() which is annotated with a javax Nullable, verify return value with requireNonNull.
     */
    public void func3() {
        Object a = get2(); // @Nullable is @Nonnull(when = When.UNKNOWN) so it seems OK that we're not reporting a bug
        Object b = requireNonNull(get2()); // true negative
        Object c = requireNonNull(
                get2()); // true negative
        System.out.println(a.toString());
        System.out.println(b.toString());
        System.out.println(c.toString());
    }

    public void multiArgMultiLineUsingNullableWorks() {
        Object result = Objects.requireNonNull(
                get2(), // seems to work with method returns @Nullable, not sure this is being validated possibly
                "This fails when the return type is string");
        System.out.println(result.toString());
    }

    public void singleArgMultiLineUsingCheckForNullWorks() {
        Object result = Objects.requireNonNull(
                methodUsingCheckForNull()); // seems okay on new line if using the single arg variant
        System.out.println(result.toString());
    }

    public void multiArgWithJustMessageOnNewLineCheckForNullWorks() {
        Object result = Objects.requireNonNull(methodUsingCheckForNull(),
                "This fails when the return type is string");
        System.out.println(result.toString());
    }

    /**
     * Main difference here is arguments are on a new line + using multi-arg variant,
     * see {@link #multiArgWithJustMessageOnNewLineCheckForNullWorks()} which works with the main difference
     * being the first param is not on a new line
     */
    public void multiArgWithArgumentsOnNewLineFails() {
        Object result = Objects.requireNonNull(
                methodUsingCheckForNull(), "This fails when the return type is string");
        System.out.println(result.toString());
    }

    /**
     * Pretty much the same as {@link #multiArgWithArgumentsOnNewLineFails()} but each argument is on a new line
     */
    public void multiArgMultiLineUsingCheckForNullFails() {
        Object result = Objects.requireNonNull(
                methodUsingCheckForNull(), // doesn't like if the method uses @CheckForNull
                "This fails when the return type is string");
        System.out.println(result.toString());
    }

    @MyNullable
    public Object get() {
        return null;
    }

    @Nullable
    public Object get2() {
        return null;
    }

    @TypeQualifierNickname
    @javax.annotation.Nonnull(when = When.MAYBE)
    @Retention(RetentionPolicy.CLASS)
    public @interface MyNullable {
    }

    @CheckForNull
    private Object methodUsingCheckForNull() {
        return null;
    }
}
