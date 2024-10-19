package edu.umd.cs.findbugs.classfile;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import org.apache.bcel.classfile.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Tomas Polesovsky
 */
class MethodsCallOrderTest {

    private PrintingBugReporter bugReporter;
    private IClassFactory classFactory;
    private IClassPath classPath;

    @BeforeEach
    void setUp() {
        bugReporter = new PrintingBugReporter();
        classFactory = ClassFactory.instance();
        classPath = classFactory.createClassPath();

        IAnalysisCache analysisCache = classFactory.createAnalysisCache(classPath, bugReporter);
        Global.setAnalysisCacheForCurrentThread(analysisCache);
        FindBugs2.registerBuiltInAnalysisEngines(analysisCache);

        Project project = new Project();
        AnalysisContext analysisContext = new AnalysisContext(project);
        AnalysisContext.setCurrentAnalysisContext(analysisContext);
    }

    @AfterEach
    void tearDown() {
        Global.setAnalysisCacheForCurrentThread(null);
    }

    void load(String codeBaseLocation) throws CheckedAnalysisException, IOException, InterruptedException {
        IClassPathBuilder builder = classFactory.createClassPathBuilder(bugReporter);
        builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(codeBaseLocation), true);
        builder.build(classPath, new NoOpFindBugsProgress());
    }

    @Test
    void testSimpleTest() {
        try {
            load("../spotbugsTestCases/build/classes/java/main/MethodsCallOrder$SimpleTest.class");
        } catch (CheckedAnalysisException | IOException | InterruptedException e) {
            fail("Unable to add MethodsCallOrder$SimpleTest.class to the build: " + e.getMessage(), e);
        }

        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor("MethodsCallOrder$SimpleTest");

        List<String> actual = new ArrayList<>();

        try {
            ClassContext classContext = Global.getAnalysisCache().getClassAnalysis(ClassContext.class, classDescriptor);
            for (Method method : classContext.getMethodsInCallOrder()) {
                actual.add(method.getName() + method.getSignature());
            }
        } catch (CheckedAnalysisException e) {
            fail("Unable to get ClassContext analysis for MethodsCallOrder: " + e.getMessage(), e);
        }

        List<String> expected = Arrays.asList(new String[] {
            "method4()V",
            "method3()V",
            "method2()V",
            "method1()V",
            "<init>(Ljava/lang/Object;)V",
            "<init>()V",
            "staticMethod4()V",
            "staticMethod3()V",
            "staticMethod2()V",
            "staticMethod1()V",
            "<clinit>()V" });

        assertEquals(expected, actual);
    }

    @Test
    void testShortcutPathTest() {
        try {
            load("../spotbugsTestCases/build/classes/java/main/MethodsCallOrder$ShortcutPathTest.class");
        } catch (CheckedAnalysisException | IOException | InterruptedException e) {
            fail("Unable to add MethodsCallOrder$SimpleTest.class to the build: " + e.getMessage(), e);
        }

        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor("MethodsCallOrder$ShortcutPathTest");

        List<String> actual = new ArrayList<>();

        try {
            ClassContext classContext = Global.getAnalysisCache().getClassAnalysis(ClassContext.class, classDescriptor);
            for (Method method : classContext.getMethodsInCallOrder()) {
                actual.add(method.getName() + method.getSignature());
            }
        } catch (CheckedAnalysisException e) {
            fail("Unable to get ClassContext analysis for MethodsCallOrder: " + e.getMessage(), e);
        }

        List<String> expected = Arrays.asList(new String[] {
            "methodC()V",
            "methodB()V",
            "methodA()V",
            "<init>()V"
        });

        assertEquals(expected, actual);
    }
}
