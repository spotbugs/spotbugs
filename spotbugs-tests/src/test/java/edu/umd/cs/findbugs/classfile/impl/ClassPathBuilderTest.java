package edu.umd.cs.findbugs.classfile.impl;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ClassPathBuilderTest {

    @Rule
    public SpotBugsRule analyzer = new SpotBugsRule();

    @Test
    public void nestedTraversalDisabled() {
        BugCollection results = analyzer.performAnalysis((engine) -> {
            engine.setScanNestedArchives(false);
            engine.setNoClassOk(true);
        }, Paths.get("../spotbugsTestCases/archives/nestedArchive.jar"));
        AppVersion appInformation = results.getCurrentAppVersion();
        assertThat(appInformation.getNumClasses(), equalTo(0));
    }

    @Test
    public void nestedTraversalEnabled() {
        BugCollection results = analyzer.performAnalysis((engine) -> engine.setScanNestedArchives(true),
                Paths.get("../spotbugsTestCases/archives/nestedArchive.jar"));
        AppVersion appInformation = results.getCurrentAppVersion();
        assertThat(appInformation.getNumClasses(), equalTo(5));
    }
}
