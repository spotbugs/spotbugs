package edu.umd.cs.findbugs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextUICommandLineTest {
    @Test
    void duplicateFilepathIsIgnored() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".xml");
        TextUICommandLine commandLine = new TextUICommandLine();

        TextUIBugReporter call1 = commandLine.initializeReporter("withMessages=" + file.toFile().getAbsolutePath(),
                () -> new SortingBugReporter(), (reporter, reporterOptions) -> {
                });
        TextUIBugReporter call2 = commandLine.initializeReporter("withMessages=" + file.toFile().getAbsolutePath(),
                () -> new SortingBugReporter(), (reporter, reporterOptions) -> {
                });

        assertNotNull(call1);
        assertNull(call2);
    }

    @Test
    void differentFilepathsOnSameReporter_createsTwoReporters() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".xml");
        Path file2 = Files.createTempFile("spotbugs", ".xml");
        TextUICommandLine commandLine = new TextUICommandLine();

        TextUIBugReporter call1 = commandLine.initializeReporter("withMessages=" + file.toFile().getAbsolutePath(),
                () -> new SortingBugReporter(), (reporter, reporterOptions) -> {
                });
        TextUIBugReporter call2 = commandLine.initializeReporter("withMessages=" + file2.toFile().getAbsolutePath(),
                () -> new SortingBugReporter(), (reporter, reporterOptions) -> {
                });

        assertNotNull(call1);
        assertNotNull(call2);
    }

    @Test
    void handleOutputFilePathUsesGzip() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".xml.gz");
        TextUICommandLine commandLine = new TextUICommandLine();
        TextUIBugReporter reporter = commandLine.initializeReporter("withMessages=" + file.toFile().getAbsolutePath(),
                () -> new SortingBugReporter(),
                (constructed, reporterOptions) -> {
                });

        reporter.finish();
        byte[] written = Files.readAllBytes(file);
        assertThat("GZip file should have 31 as its header", written[0], is((byte) 31));
        assertThat("GZip file should have -117 as its header", written[1], is((byte) -117));
    }

    @Test
    void handleOutputFileTruncatesExisting() throws IOException {
        Path file = Files.createTempFile("spotbugs", ".html");
        Files.writeString(file, "content");
        TextUICommandLine commandLine = new TextUICommandLine();
        TextUIBugReporter reporter = commandLine.initializeReporter("withMessages=" + file.toFile().getAbsolutePath(),
                () -> new SortingBugReporter(),
                (constructed, reporterOptions) -> {
                });

        reporter.finish();
        byte[] written = Files.readAllBytes(file);
        assertThat("Output file should be truncated to 0 bytes", written.length, is(0));
    }

    @Test
    void htmlReportWithOption() throws IOException {
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
