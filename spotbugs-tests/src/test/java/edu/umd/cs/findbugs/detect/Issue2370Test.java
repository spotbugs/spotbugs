package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;

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
        assertNoBugType("DMI_RANDOM_USED_ONLY_ONCE");
    }
}
