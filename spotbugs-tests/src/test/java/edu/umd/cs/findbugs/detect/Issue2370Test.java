package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue2370Test extends AbstractIntegrationTest {

    private String classLocation(String className) {
        return String.format("ghIssues/issue2370/%s.class", className);
    }

    private static Stream<Arguments> classes() {
        return Stream.of(
                Arguments.of("Issue2370Doubles"),
                Arguments.of("Issue2370Ints"),
                Arguments.of("Issue2370Longs"));
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
