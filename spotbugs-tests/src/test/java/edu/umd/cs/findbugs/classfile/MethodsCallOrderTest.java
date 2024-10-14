package edu.umd.cs.findbugs.classfile;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import org.apache.bcel.classfile.Method;
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
public class MethodsCallOrderTest {

    @Test
    void testSimpleName() {
        PrintingBugReporter bugReporter = new PrintingBugReporter();
        IClassFactory classFactory = ClassFactory.instance();
        IClassPath classPath = classFactory.createClassPath();

        IAnalysisCache analysisCache = classFactory.createAnalysisCache(classPath, bugReporter);
        Global.setAnalysisCacheForCurrentThread(analysisCache);
        FindBugs2.registerBuiltInAnalysisEngines(analysisCache);

        Project project = new Project();
        AnalysisContext analysisContext = new AnalysisContext(project);
        AnalysisContext.setCurrentAnalysisContext(analysisContext);

        IClassPathBuilder builder = classFactory.createClassPathBuilder(bugReporter);
        builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator("../spotbugsTestCases/build/classes/java/main/MethodsCallOrder.class"),
                true);
        try {
            builder.build(classPath, new NoOpFindBugsProgress());
        } catch (CheckedAnalysisException | IOException | InterruptedException e) {
            fail("Unable to add MethodsCallOrder.class to the build: " + e.getMessage(), e);
        }

        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor("MethodsCallOrder");

        List<String> actual = new ArrayList<>();

        try {
            ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, classDescriptor);
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
}
