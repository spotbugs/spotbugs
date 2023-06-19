package edu.umd.cs.findbugs;

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
    public void handleOutputFileTruncatesExisting() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".html");
        Files.writeString(file, "content");
        TextUICommandLine commandLine = new TextUICommandLine();
        SortingBugReporter reporter = new SortingBugReporter();
        commandLine.handleOutputFilePath(reporter, "withMessages=" + file.toFile().getAbsolutePath());

        reporter.finish();
        byte[] written = Files.readAllBytes(file);
        assertThat("Output file should be truncated to 0 bytes", written.length, is(0));
    }

    @Test
    public void htmlReportWithOption() throws IOException {
        Path xmlFile = Files.createTempFile("spotbugs", ".xml");
        Path htmlFile = Files.createTempFile("spotbugs", ".html");
        Path sarifFile = Files.createTempFile("spotbugs", ".sarif");
        Path emacsFile = Files.createTempFile("spotbugs", ".emacs");
        Path xdocsFile = Files.createTempFile("spotbugs", ".xdocs");
        Path textFile = Files.createTempFile("spotbugs", ".txt");

        TextUICommandLine commandLine = new TextUICommandLine();
        try (FindBugs2 findbugs = new FindBugs2()) {
            commandLine.handleOption("-xml", "=" + xmlFile.toFile().getAbsolutePath());
            commandLine.handleOption("-html", "fancy-hist.xsl=" + htmlFile.toFile().getAbsolutePath());
            commandLine.handleOption("-sarif", "=" + sarifFile.toFile().getAbsolutePath());
            commandLine.handleOption("-emacs", "=" + emacsFile.toFile().getAbsolutePath());
            commandLine.handleOption("-xdocs", "=" + xdocsFile.toFile().getAbsolutePath());
            commandLine.handleOption("-sortByClass", "=" + textFile.toFile().getAbsolutePath());
            commandLine.configureEngine(findbugs);
            findbugs.getBugReporter().finish();
        }
        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        assertThat(html, containsString("#historyTab"));

        assertTrue(xmlFile.toFile().isFile());
        assertTrue(sarifFile.toFile().isFile());
        assertTrue(emacsFile.toFile().isFile());
        assertTrue(xdocsFile.toFile().isFile());
        assertTrue(textFile.toFile().isFile());
    }
}
