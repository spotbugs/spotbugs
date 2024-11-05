package edu.umd.cs.findbugs.classfile.impl;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpotBugsExtension.class)
class ClassPathBuilderTest {

    @Test
    void nestedTraversalDisabled(SpotBugsRunner spotbugs) {
        BugCollection results = spotbugs.performAnalysis(engine -> {
            engine.setScanNestedArchives(false);
            engine.setNoClassOk(true);
        }, Paths.get("../spotbugsTestCases/archives/nestedArchive.jar"));
        AppVersion appInformation = results.getCurrentAppVersion();
        assertThat(appInformation.getNumClasses(), equalTo(0));
    }

    @Test
    void nestedTraversalEnabled(SpotBugsRunner spotbugs) {
        BugCollection results = spotbugs.performAnalysis(engine -> engine.setScanNestedArchives(true),
                Paths.get("../spotbugsTestCases/archives/nestedArchive.jar"));
        AppVersion appInformation = results.getCurrentAppVersion();
        assertThat(appInformation.getNumClasses(), equalTo(5));
    }
}
