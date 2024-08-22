package edu.umd.cs.findbugs.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CommandLineTest {
    @Test
    void testNoExtraPart() {
        CommandLine.Option option = CommandLine.splitOption("-html");
        assertThat(option.option, is("-html"));
        assertThat(option.extraPart, is(""));
    }

    @Test
    void testSplitOptionOnLinux() {
        CommandLine.Option option = CommandLine.splitOption("-html=/path/to/spotbugs.html");
        assertThat(option.option, is("-html"));
        assertThat(option.extraPart, is("=/path/to/spotbugs.html"));
    }

    @Test
    void testSplitOptionWithColonOnLinux() {
        CommandLine.Option option = CommandLine.splitOption("-html:default.xsl=/path/to/spotbugs.html");
        assertThat(option.option, is("-html"));
        assertThat(option.extraPart, is("default.xsl=/path/to/spotbugs.html"));
    }

    @Test
    void testSplitOptionOnWindows() {
        CommandLine.Option option = CommandLine.splitOption("-html=C:\\path\\to\\spotbugs.html");
        assertThat(option.option, is("-html"));
        assertThat(option.extraPart, is("=C:\\path\\to\\spotbugs.html"));
    }

    @Test
    void testSplitOptionWithColonOnWindows() {
        CommandLine.Option option = CommandLine.splitOption("-html:default.xsl=C:\\path\\to\\spotbugs.html");
        assertThat(option.option, is("-html"));
        assertThat(option.extraPart, is("default.xsl=C:\\path\\to\\spotbugs.html"));
    }
}
