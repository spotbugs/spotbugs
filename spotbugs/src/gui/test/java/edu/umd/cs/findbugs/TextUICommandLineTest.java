package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.config.CommandLine;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TextUICommandLineTest {
    @Test
    public void handleOutputFileReturnsRemainingPart() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".xml");
        TextUICommandLine commandLine = new TextUICommandLine();
        SortingBugReporter reporter = new SortingBugReporter();
        String result = commandLine.handleOutputFilePath(reporter, "withMessages=" + file.toFile().getAbsolutePath());

        assertThat(result, is("withMessages"));
    }

    @Test
    public void handleOutputFilePathUsesGzip() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".xml.gz");
        TextUICommandLine commandLine = new TextUICommandLine();
        SortingBugReporter reporter = new SortingBugReporter();
        commandLine.handleOutputFilePath(reporter, "withMessages=" + file.toFile().getAbsolutePath());

        reporter.finish();
        byte[] written = Files.readAllBytes(file);
        assertThat("GZip file should have 31 as its header", written[0], is((byte) 31));
        assertThat("GZip file should have -117 as its header", written[1], is((byte) -117));
    }

    @Test
    public void htmlReportWithOption() throws IOException, InterruptedException {
        Path htmlFile = Files.createTempFile("spotbugs", ".html");
        Path sarifFile = Files.createTempFile("spotbugs", ".sarif");
        TextUICommandLine commandLine = new TextUICommandLine();
        try (FindBugs2 findbugs = new FindBugs2()) {
            commandLine.handleOption("-html", "fancy-hist.xsl=" + htmlFile.toFile().getAbsolutePath());
            commandLine.handleOption("-sarif", "=" + sarifFile.toFile().getAbsolutePath());
            commandLine.configureEngine(findbugs);
            findbugs.getBugReporter().finish();
        }
        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        assertThat(html, containsString("#historyTab"));
        assertTrue(sarifFile.toFile().isFile());
    }
}
