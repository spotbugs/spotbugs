package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.ClassPathImpl;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PlaceholderTest {
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

    @Test
    public void testFormatWithKey() throws ClassNotFoundException {
        BugPattern bugPattern = new BugPattern("BUG_TYPE", "abbrev", "category", false, "describing about this bug type...",
                "describing about this bug type with value {0.givenClass} and {1.name}", "detailText", null, 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        JavaClass clazz = Repository.lookupClass(PlaceholderTest.class);
        Method method = Arrays.stream(clazz.getMethods()).filter(m -> m.getName().equals("testFormatWithKey")).findFirst().get();
        reporter.reportBug(new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addClassAndMethod(clazz, method));
        reporter.finish();

        String json = writer.toString();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject run = jsonObject.getJSONArray("runs").getJSONObject(0);
        JSONArray rules = run.getJSONObject("tool").getJSONObject("driver").getJSONArray("rules");
        String defaultText = rules.getJSONObject(0).getJSONObject("messageStrings").getJSONObject("default").getString("text");
        assertThat("key in placeholders are removed",
                defaultText, is("describing about this bug type with value {0} and {1}"));

        JSONArray results = run.getJSONArray("results");
        JSONObject message = results.getJSONObject(0).getJSONObject("message");
        JSONArray arguments = message.getJSONArray("arguments");
        assertThat("BugAnnotation has been formatted by the key in placeholder",
                arguments.getString(0), is("PlaceholderTest"));
        assertThat("BugAnnotation has been formatted by the key in placeholder",
                arguments.getString(1), is("testFormatWithKey"));
    }
}
