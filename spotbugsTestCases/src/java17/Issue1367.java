/**
 * A record class to reproduce {@code EQ_UNUSUAL} false positive.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1367">The related GitHub issue</a>
 */
public record Issue1367 (int value) {}
