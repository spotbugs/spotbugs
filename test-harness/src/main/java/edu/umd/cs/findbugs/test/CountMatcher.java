package edu.umd.cs.findbugs.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @since ?
 *
 * @param <T>
 *            matcher item type
 */
public final class CountMatcher<T> extends TypeSafeMatcher<Iterable<T>> {

    private final int minCount;
    private final int maxCount;
    private final Matcher<T> matcher;

    public CountMatcher(int minCount, int maxCount, Matcher<T> matcher) {
        this.minCount = minCount;
        this.maxCount = maxCount;
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
    public static <T> Matcher<Iterable<T>> containsExactly(final int count, final Matcher<T> matcher) {
        return new CountMatcher<>(count, count, matcher);
    }

    /**
     * Creates a matcher for {@link Iterable}s that only matches if at least {@code minCount} and at most {@code maxCount}
     * items (both inclusive) match the specified {@code matcher}.
     *
     * @param matcher
     *            A non-{@code null} matcher that must match at least {@code minCount} and at most {@code maxCount} times.
     * @param <T>
     *            matcher item type
     * @param minCount
     *            How many times the {@code matcher} must match at least (inclusive).
     * @param maxCount
     *            How many times the {@code matcher} must match at most (inclusive).
     * @return new matcher instance
     */
    public static <T> Matcher<Iterable<T>> containsBetween(final int minCount, final int maxCount, final Matcher<T> matcher) {
        return new CountMatcher<>(minCount, maxCount, matcher);
    }

    @Override
    protected boolean matchesSafely(Iterable<T> iterable) {
        int numberOfmatches = 0;

        for (final Object item : iterable) {
            if (matcher.matches(item)) {
                numberOfmatches++;
            }
        }

        return numberOfmatches >= minCount && numberOfmatches <= maxCount;
    }

    @Override
    public void describeTo(final Description desc) {
        if (minCount == maxCount) {
            desc.appendText("Iterable containing exactly ").appendValue(minCount).appendText(" ").appendDescriptionOf(matcher);
        } else {
            desc.appendText("Iterable containing at least ").appendValue(minCount)
                    .appendText(" and at most ").appendValue(maxCount).appendText(" ").appendDescriptionOf(matcher);
        }
    }

    @Override
    protected void describeMismatchSafely(Iterable<T> items, Description mismatchDescription) {
        if (!items.iterator().hasNext()) {
            mismatchDescription.appendText("The collection was empty");
        } else {
            for (final Object item : items) {
                mismatchDescription.appendText("\n");
                if (matcher.matches(item)) {
                    mismatchDescription.appendText("Match:   ");
                } else {
                    mismatchDescription.appendText("Mismatch:");
                }
                matcher.describeMismatch(item, mismatchDescription);
            }
        }
    }
}
