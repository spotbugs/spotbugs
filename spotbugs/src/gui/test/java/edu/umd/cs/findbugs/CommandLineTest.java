package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.config.CommandLine;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class CommandLineTest {
    private EmptyCommandLine commandLine;

    @Before
    public void createCommandLine() {
        commandLine = new EmptyCommandLine();
        commandLine.addSwitchWithOptionalExtraPart("-xml", "withMessages", "");
    }

    @Test
    public void parseOpt() throws CommandLine.HelpRequestedException, IOException {
        commandLine.parse(new String[] { "-xml" });

        assertThat(commandLine.options.get(0), is("-xml"));
        assertThat(commandLine.extraParts.get(0), is(""));
    }

    @Test
    public void parseOptWithExtraPart() throws CommandLine.HelpRequestedException, IOException {
        commandLine.parse(new String[] { "-xml:withMessages" });

        assertThat(commandLine.options.get(0), is("-xml"));
        assertThat(commandLine.extraParts.get(0), is("withMessages"));
    }

    @Test
    public void parseOptWithFilePath() throws CommandLine.HelpRequestedException, IOException {
        commandLine.parse(new String[] { "-xml=spotbugs.xml" });

        assertThat(commandLine.options.get(0), is("-xml"));
        assertThat(commandLine.extraParts.get(0), is("=spotbugs.xml"));
    }

    @Test
    public void parseOptWithExtraPartAndFilePath() throws CommandLine.HelpRequestedException, IOException {
        commandLine.parse(new String[] { "-xml:withMessages=spotbugs.xml" });

        assertThat(commandLine.options.get(0), is("-xml"));
        assertThat(commandLine.extraParts.get(0), is("withMessages=spotbugs.xml"));
    }

    private static class EmptyCommandLine extends CommandLine {
        final List<String> options = new ArrayList<>();
        final List<String> extraParts = new ArrayList<>();

        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            options.add(option);
            extraParts.add(optionExtraPart);
        }

        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
        }
    }
}
