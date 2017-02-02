package edu.umd.cs.findbugs;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

final class CountMatcher<T> extends BaseMatcher<Iterable<T>> {
    private final int count;
    private final Matcher<T> matcher;

    /**
     * @param count
     * @param matcher
     */
    CountMatcher(int count, Matcher<T> matcher) {
        this.count = count;
        this.matcher = matcher;
    }

    @Override
    public boolean matches(final Object obj) {
        int matches = 0;

        if (obj instanceof Iterable<?>) {
            final Iterable<?> it = (Iterable<?>) obj;
            for (final Object o : it) {
                if (matcher.matches(o)) {
                    matches++;
                }
            }
        }

        return matches == count;
    }

    @Override
    public void describeTo(final Description desc) {
        desc.appendText("Iterable containing exactly " + count + " ").appendDescriptionOf(matcher);
    }
}