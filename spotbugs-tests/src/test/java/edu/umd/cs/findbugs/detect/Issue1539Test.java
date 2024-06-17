package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/pull/1540">PR suggested this test case</a>
 */
class Issue1539Test extends AbstractIntegrationTest {

    private String classLocation(String className) {
        return String.format("ghIssues/issue1539/%s.class", className);
    }

    private static Stream<Arguments> classes() {
        return Stream.of(
                Arguments.of("Issue1539Instance"),
                Arguments.of("Issue1539Static"),
                Arguments.of("Issue1539ThreadLocal"),
                Arguments.of("Issue1539Argument"));
    }

    @ParameterizedTest
    @MethodSource("classes")
    void testInstance(String className) {
        performAnalysis(classLocation(className));
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DMI_RANDOM_USED_ONLY_ONCE")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
