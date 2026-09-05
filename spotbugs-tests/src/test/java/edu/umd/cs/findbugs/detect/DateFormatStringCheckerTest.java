/**
 *
 */
package edu.umd.cs.findbugs.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author gtoison
 */
public class DateFormatStringCheckerTest {

    private final DateFormatStringChecker checker = new DateFormatStringChecker(null);

    @ParameterizedTest
    @MethodSource("testCases")
    void removeNonInterpretedText(String expected, String source) {
        assertEquals(expected, checker.removeNonInterpretedText(source));
    }

    static Stream<Arguments> testCases() {
        return Arrays.asList(
                Arguments.of("dd/MM/yyyy", "dd/MM/yyyy"),
                Arguments.of("ddMMyyyy", "ddMMyyyy"),
                Arguments.of("d MMMM yyyy à HHmm", "d MMMM yyyy à HH'h'mm"),
                Arguments.of("ddMMyyyy ", "ddMMyyyy 'non interpreted text'"),
                Arguments.of("HH", "HH''"),
                Arguments.of("HH", "HH'o''clock'")).stream();
    }
}
