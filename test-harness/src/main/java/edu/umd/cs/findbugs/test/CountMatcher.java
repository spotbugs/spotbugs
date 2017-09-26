package edu.umd.cs.findbugs.test;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @since ?
 *
 * @param <T>
 *            matcher item type
 */
public final class CountMatcher<T> extends TypeSafeMatcher<Iterable<T>> {

    private final int count;
    private final Matcher<T> matcher;

    public CountMatcher(int count, Matcher<T> matcher) {
        this.count = count;
        this.matcher = matcher;
    }

    /**
     * Creates a matcher for {@link Iterable}s that only matches if exactly {@code count} items match the specified
     * {@code matcher}.
     *
     * @param matcher
     *            A non-{@code null} matcher that must match exactly {@code count} times.
     * @param <T>
     *            matcher item type
     * @param count
     *            How many times the {@code matcher} must match.
     * @return new matcher instance
     */
    @Factory
    public static <T> Matcher<Iterable<T>> containsExactly(final int count, final Matcher<T> matcher) {
        return new CountMatcher<>(count, matcher);
    }

    @Override
    protected boolean matchesSafely(Iterable<T> iterable) {
        int numberOfmatches = 0;

        for (final Object item : iterable) {
            if (matcher.matches(item)) {
                numberOfmatches++;
            }
        }

        return numberOfmatches == count;
    }

    @Override
    public void describeTo(final Description desc) {
        desc.appendText("Iterable containing exactly ").appendValue(count).appendText(" ").appendDescriptionOf(matcher);
    }
}
