package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class SarifBugReporterTest {
    private Project project;
    private SarifBugReporter reporter;
    private StringWriter writer;

    @Before
    public void setup() {
        project = new Project();
        reporter = new SarifBugReporter(project);
        writer = new StringWriter();
        reporter.setWriter(new PrintWriter(writer));
    }

    @After
    public void teardown() {
        project.close();
    }

    /**
     * Root object&apos;s first field should be {@code "version"}, and it should be {@code "2.1.0"}.
     * Root object also should have {@code "$schema"} field that points the JSON schema provided by SARIF community.
     */
    @Test
    public void testVersionAndSchema() {
        reporter.finish();

        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);

        assertThat("the first key in JSON should be 'version'", json, startsWith("{\"version\""));
        assertThat(jsonObject.get("version"), is("2.1.0"));
        assertThat(jsonObject.get("$schema"), is("https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.4.json"));
    }

    /**
     * {@code toolComponent} object in {@code "runs.tool.driver"} SHOULD have {@code "version"} (§3.19.2).
     * A toolComponent object SHALL contain a {@code "name"} property (§3.19.8).
     * A toolComponent object MAY contain a {@code "language"} property (§3.19.21).
     */
    @Test
    public void testDriver() {
        final String EXPECTED_NAME = "SpotBugs JUnit Test";
        final String EXPECTED_VERSION = "1.2.3";
        final String EXPECTED_LANGUAGE = "ja";
        Version.registerApplication(EXPECTED_NAME, EXPECTED_VERSION);

        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPANESE);
            reporter.finish();
        } finally {
            Locale.setDefault(defaultLocale);
        }

        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONObject tool = run.getJSONObject("tool");
        JSONObject driver = tool.getJSONObject("driver");

        assertThat(driver.get("name"), is(EXPECTED_NAME));
        assertThat(driver.get("version"), is(EXPECTED_VERSION));
        assertThat(driver.get("language"), is(EXPECTED_LANGUAGE));
    }
}
