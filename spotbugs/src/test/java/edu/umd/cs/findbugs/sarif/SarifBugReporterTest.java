package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Project;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

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

    @Test
    public void testVersionAndSchema() {
        reporter.finish();

        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);

        assertThat("the first key in JSON should be 'version'", json, startsWith("{\"version\""));
        assertThat(jsonObject.get("version"), is("2.1.0"));
        assertThat(jsonObject.get("$schema"), is("https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.4.json"));
    }
}
