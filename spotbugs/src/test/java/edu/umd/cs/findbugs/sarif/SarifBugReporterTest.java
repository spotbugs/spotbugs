package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.ClassPathImpl;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

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
        AnalysisContext.removeCurrentAnalysisContext();
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
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

        assertThat("the first key in JSON should be 'version'", json, startsWith("{\"version\""));
        assertThat(jsonObject.get("version").getAsString(), is("2.1.0"));
        assertThat(jsonObject.get("$schema").getAsString(), is(
                "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json"));
    }

    /**
     * {@code toolComponent} object in {@code "runs.tool.driver"} SHOULD have {@code "version"} (§3.19.2).
     * A toolComponent object SHALL contain a {@code "name"} property (§3.19.8).
     * A toolComponent object MAY contain a {@code "language"} property (§3.19.21).
     */
    @Test
    public void testDriver() {
        final String EXPECTED_VERSION = Version.VERSION_STRING;
        final String EXPECTED_LANGUAGE = "ja";

        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPANESE);
            reporter.finish();
        } finally {
            Locale.setDefault(defaultLocale);
        }

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = (JsonObject) jsonObject.getAsJsonArray("runs").get(0);
        JsonObject tool = run.getAsJsonObject("tool");
        JsonObject driver = tool.getAsJsonObject("driver");

        assertThat(driver.get("name").getAsString(), is("SpotBugs"));
        assertThat(driver.get("version").getAsString(), is(EXPECTED_VERSION));
        assertThat(driver.get("language").getAsString(), is(EXPECTED_LANGUAGE));
    }

    @Test
    public void testRuleWithArguments() {
        // given
        final String EXPECTED_BUG_TYPE = "BUG_TYPE";
        final int EXPECTED_PRIORITY = Priorities.NORMAL_PRIORITY;
        final String EXPECTED_DESCRIPTION = "describing about this bug type...";
        BugPattern bugPattern = new BugPattern(EXPECTED_BUG_TYPE, "abbrev", "category", false, EXPECTED_DESCRIPTION,
                "describing about this bug type with value {0}...", "detailText", null, 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        // when
        reporter.reportBug(new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10).addClass("the/target/Class"));
        reporter.finish();

        // then
        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonArray rules = run.getAsJsonObject("tool").getAsJsonObject("driver").getAsJsonArray("rules");
        JsonArray results = run.getAsJsonArray("results");

        assertThat(rules.size(), is(1));
        JsonObject rule = rules.get(0).getAsJsonObject();
        assertThat(rule.get("id").getAsString(), is(bugPattern.getType()));
        String defaultText = rule.getAsJsonObject("messageStrings").getAsJsonObject("default").get("text").getAsString();
        assertThat(defaultText, is("describing about this bug type with value {0}..."));

        assertThat(results.size(), is(1));
        JsonObject result = results.get(0).getAsJsonObject();
        assertThat(result.get("ruleId").getAsString(), is(bugPattern.getType()));
        JsonObject message = result.getAsJsonObject("message");
        assertThat(message.get("id").getAsString(), is("default"));
        assertThat(message.get("text").getAsString(), is(bugPattern.getShortDescription()));
        JsonArray arguments = message.getAsJsonArray("arguments");
        assertThat(arguments.get(0).getAsInt(), is(10));
    }

    @Test
    public void testMissingClassNotification() {
        ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor("com/github/spotbugs/MissingClass");
        reporter.reportMissingClass(classDescriptor);
        reporter.finish();

        // then
        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonArray toolConfigurationNotifications = run.getAsJsonArray("invocations")
                .get(0).getAsJsonObject()
                .getAsJsonArray("toolConfigurationNotifications");

        assertThat(toolConfigurationNotifications.size(), is(1));
        JsonObject notification = toolConfigurationNotifications.get(0).getAsJsonObject();
        assertThat(notification.getAsJsonObject("descriptor").get("id").getAsString(), is("spotbugs-missing-classes"));
        assertThat(notification.getAsJsonObject("message").get("text").getAsString(), is(
                "Classes needed for analysis were missing: [com.github.spotbugs.MissingClass]"));
    }

    @Test
    public void testErrorNotification() {
        reporter.logError("Unexpected Error");
        reporter.finish();

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonArray toolExecutionNotifications = run.getAsJsonArray("invocations")
                .get(0).getAsJsonObject()
                .getAsJsonArray("toolExecutionNotifications");

        assertThat(toolExecutionNotifications.size(), is(1));
        JsonObject notification = toolExecutionNotifications.get(0).getAsJsonObject();
        assertThat(notification.getAsJsonObject("descriptor").get("id").getAsString(), is("spotbugs-error-0"));
        assertThat(notification.getAsJsonObject("message").get("text").getAsString(), is("Unexpected Error"));
        assertFalse(notification.has("exception"));
    }

    @Test
    public void testExceptionNotification() {
        reporter.getProject().getSourceFinder().setSourceBaseList(Collections.singletonList(new File("src/test/java").getAbsolutePath()));
        reporter.logError("Unexpected Error", new Exception("Unexpected Problem"));
        reporter.finish();

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonArray toolExecutionNotifications = run.getAsJsonArray("invocations")
                .get(0).getAsJsonObject()
                .getAsJsonArray("toolExecutionNotifications");

        assertThat(toolExecutionNotifications.size(), is(1));
        JsonObject notification = toolExecutionNotifications.get(0).getAsJsonObject();
        assertThat(notification.getAsJsonObject("descriptor").get("id").getAsString(), is("spotbugs-error-0"));
        assertThat(notification.getAsJsonObject("message").get("text").getAsString(), is("Unexpected Error"));
        assertTrue(notification.has("exception"));
        JsonArray frames = notification.getAsJsonObject("exception").getAsJsonObject("stack").getAsJsonArray("frames");
        JsonObject physicalLocation = frames.get(0).getAsJsonObject().getAsJsonObject("location").getAsJsonObject("physicalLocation");
        String uri = physicalLocation.getAsJsonObject("artifactLocation").get("uri").getAsString();
        assertThat(uri, is("edu/umd/cs/findbugs/sarif/SarifBugReporterTest.java"));
    }

    @Test
    public void testExceptionNotificationWithoutMessage() {
        reporter.logError("Unexpected Error", new Exception());
        reporter.finish();

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonArray toolExecutionNotifications = run.getAsJsonArray("invocations")
                .get(0).getAsJsonObject()
                .getAsJsonArray("toolExecutionNotifications");

        assertThat(toolExecutionNotifications.size(), is(1));
        JsonObject notification = toolExecutionNotifications.get(0).getAsJsonObject();
        assertThat(notification.getAsJsonObject("descriptor").get("id").getAsString(), is("spotbugs-error-0"));
        assertThat(notification.getAsJsonObject("message").get("text").getAsString(), is("Unexpected Error"));
        assertTrue(notification.has("exception"));
    }

    @Test
    public void testHelpUriAndTags() {
        BugPattern bugPattern = new BugPattern("TYPE", "abbrev", "category", false, "shortDescription",
                "longDescription", "detailText", "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        reporter.reportBug(new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10).addClass("the/target/Class"));
        reporter.finish();

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonArray rules = run.getAsJsonObject("tool").getAsJsonObject("driver").getAsJsonArray("rules");

        assertThat(rules.size(), is(1));
        JsonObject rule = rules.get(0).getAsJsonObject();
        assertThat(rule.get("helpUri").getAsString(), is("https://example.com/help.html#TYPE"));

        JsonArray tags = rule.getAsJsonObject("properties").getAsJsonArray("tags");
        assertThat(tags.size(), is(1));
        assertThat(tags.get(0).getAsString(), is("category"));
    }

    @Test
    public void testExtensions() throws PluginException {
        PluginLoader pluginLoader = DetectorFactoryCollection.instance().getCorePlugin().getPluginLoader();
        Plugin plugin = new Plugin("pluginId", "version", null, pluginLoader, true, false);
        DetectorFactoryCollection dfc = new DetectorFactoryCollection(plugin);
        try {
            DetectorFactoryCollection.resetInstance(dfc);
            reporter.finish();
        } finally {
            DetectorFactoryCollection.resetInstance(null);
        }

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();
        JsonObject tool = run.getAsJsonObject("tool");
        JsonArray extensions = tool.getAsJsonArray("extensions");

        assertThat(extensions.size(), is(1));
        JsonObject extension = extensions.get(0).getAsJsonObject();

        assertThat(extension.get("name").getAsString(), is("pluginId"));
        assertThat(extension.get("version").getAsString(), is("version"));
    }

    @Test
    public void testSourceLocation() throws IOException {
        Path tmpDir = Files.createTempDirectory("spotbugs");
        new File(tmpDir.toFile(), "SampleClass.java").createNewFile();
        SourceFinder sourceFinder = reporter.getProject().getSourceFinder();
        sourceFinder.setSourceBaseList(Collections.singleton(tmpDir.toString()));

        BugPattern bugPattern = new BugPattern("TYPE", "abbrev", "category", false, "shortDescription",
                "longDescription", "detailText", "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        reporter.reportBug(new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10).addClass("SampleClass"));
        reporter.finish();

        String json = writer.toString();
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonObject run = jsonObject.getAsJsonArray("runs").get(0).getAsJsonObject();

        JsonObject originalUriBaseIds = run.getAsJsonObject("originalUriBaseIds");
        String uriBaseId = takeFirstKey(originalUriBaseIds).get();
        assertThat(URI.create(originalUriBaseIds.getAsJsonObject(uriBaseId).get("uri").getAsString()), is(tmpDir.toUri()));

        JsonArray results = run.getAsJsonArray("results");
        assertThat(results.size(), is(1));
        JsonObject result = results.get(0).getAsJsonObject();
        JsonObject artifactLocation = result.getAsJsonArray("locations").get(0).getAsJsonObject().getAsJsonObject("physicalLocation").getAsJsonObject(
                "artifactLocation");
        String relativeUri = artifactLocation.get("uri").getAsString();
        assertThat("relative URI that can be resolved by the uriBase",
                relativeUri, is("SampleClass.java"));
        assertThat(artifactLocation.get("uriBaseId").getAsString(), is(uriBaseId));
    }

    Optional<String> takeFirstKey(JsonObject object) {
        return object.keySet().stream().findFirst();
    }
}
