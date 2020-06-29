package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.ClassPathImpl;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SarifBugReporterTest {
    private SarifBugReporter reporter;
    private StringWriter writer;

    @Before
    public void setup() {
        Project project = new Project();
        reporter = new SarifBugReporter(project);
        writer = new StringWriter();
        reporter.setWriter(new PrintWriter(writer));
        reporter.setPriorityThreshold(Priorities.IGNORE_PRIORITY);
        DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection());
        IAnalysisCache analysisCache = ClassFactory.instance().createAnalysisCache(new ClassPathImpl(), reporter);
        Global.setAnalysisCacheForCurrentThread(analysisCache);
        FindBugs2.registerBuiltInAnalysisEngines(analysisCache);
        AnalysisContext analysisContext = new AnalysisContext(project) {
            public boolean isApplicationClass(@DottedClassName String className) {
                // treat all classes as application class, to report bugs in it
                return true;
            }
        };
        AnalysisContext.setCurrentAnalysisContext(analysisContext);
    }

    @After
    public void teardown() {
        AnalysisContext.currentAnalysisContext().close();
        Global.removeAnalysisCacheForCurrentThread();
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

    @Test
    public void testRuleWithArguments() {
        // given
        final String EXPECTED_BUG_TYPE = "BUG_TYPE";
        final int EXPECTED_PRIORITY = Priorities.NORMAL_PRIORITY;
        final String EXPECTED_DESCRIPTION = "describing about this bug type...";
        BugPattern bugPattern = new BugPattern(EXPECTED_BUG_TYPE, "addrev", "category", false, EXPECTED_DESCRIPTION, "describing about this bug type with value {0}...", "detailText", null, 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        // when
        reporter.reportBug(new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10).addClass("TheTargetClass"));
        reporter.finish();

        // then
        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONArray rules = run.getJSONObject("tool").getJSONObject("driver").getJSONArray("rules");
        JSONArray results = run.getJSONArray("results");

        assertThat(rules.length(), is(1));
        JSONObject rule = rules.getJSONObject(0);
        assertThat(rule.get("id"), is(bugPattern.getType()));
        String defaultText = rule.getJSONObject("messageStrings").getJSONObject("default").getString("text");
        assertThat(defaultText, is("describing about this bug type with value {0}..."));

        assertThat(results.length(), is(1));
        JSONObject result = results.getJSONObject(0);
        assertThat(result.get("ruleId"), is(bugPattern.getType()));
        JSONObject message = result.getJSONObject("message");
        assertThat(message.getString("id"), is("default"));
        JSONArray arguments = message.getJSONArray("arguments");
        assertThat(arguments.getInt(0), is(10));
    }

    @Test
    public void testMissingClassNotification() {
        ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor("com/github/spotbugs/MissingClass");
        reporter.reportMissingClass(classDescriptor);
        reporter.finish();

        // then
        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONObject tool = run.getJSONObject("tool");
        JSONObject driver = tool.getJSONObject("driver");
        JSONArray notifications = driver.getJSONArray("notifications");

        assertThat(notifications.length(), is(1));
        JSONObject notification = notifications.getJSONObject(0);
        assertThat(notification.getJSONObject("descriptor").getString("id"), is("spotbugs-missing-classes"));
        assertThat(notification.getJSONObject("message").getString("text"), is("Classes needed for analysis were missing: [com.github.spotbugs.MissingClass]"));
    }

    @Test
    public void testErrorNotification() {
        reporter.logError("Unexpected Error");
        reporter.finish();

        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONObject tool = run.getJSONObject("tool");
        JSONObject driver = tool.getJSONObject("driver");
        JSONArray notifications = driver.getJSONArray("notifications");

        assertThat(notifications.length(), is(1));
        JSONObject notification = notifications.getJSONObject(0);
        assertThat(notification.getJSONObject("descriptor").getString("id"), is("spotbugs-error-0"));
        assertThat(notification.getJSONObject("message").getString("text"), is("Unexpected Error"));
        assertFalse(notification.has("exception"));
    }

    @Test
    public void testExceptionNotification() {
        reporter.logError("Unexpected Error", new Exception("Unexpected Problem"));
        reporter.finish();

        String json = writer.toString();
        System.err.println(json);
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONObject tool = run.getJSONObject("tool");
        JSONObject driver = tool.getJSONObject("driver");
        JSONArray notifications = driver.getJSONArray("notifications");

        assertThat(notifications.length(), is(1));
        JSONObject notification = notifications.getJSONObject(0);
        assertThat(notification.getJSONObject("descriptor").getString("id"), is("spotbugs-error-0"));
        assertThat(notification.getJSONObject("message").getString("text"), is("Unexpected Error"));
        assertTrue(notification.has("exception"));
    }

    @Test
    public void testExceptionNotificationWithoutMessage() {
        reporter.logError("Unexpected Error", new Exception());
        reporter.finish();

        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONObject tool = run.getJSONObject("tool");
        JSONObject driver = tool.getJSONObject("driver");
        JSONArray notifications = driver.getJSONArray("notifications");

        assertThat(notifications.length(), is(1));
        JSONObject notification = notifications.getJSONObject(0);
        assertThat(notification.getJSONObject("descriptor").getString("id"), is("spotbugs-error-0"));
        assertThat(notification.getJSONObject("message").getString("text"), is("Unexpected Error"));
        assertTrue(notification.has("exception"));
    }
}
