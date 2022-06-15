package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindInstanceLockOnSharedStaticDataCheckTest extends AbstractIntegrationTest {

    @Test
    public void findSSDBugInClass_InstanceLevelLockOnSharedStaticData() {
        performAnalysis("instanceLockOnSharedStaticData/InstanceLevelLockObject.class");

        assertNumOfSSDBugs(1);

        assertSSDBug("InstanceLevelLockObject", "methodWithBug", 12);
    }

    @Test
    public void findSSDBugInClass_InstanceLevelSynchronizedMethod() {
        performAnalysis("instanceLockOnSharedStaticData/InstanceLevelSynchronizedMethod.class");

        assertNumOfSSDBugs(1);

        assertSSDBug("InstanceLevelSynchronizedMethod", "methodWithBug", 10);
    }

    @Test
    public void findNoSSDBugInClass_StaticLockInsideNotStaticSynchronizedMethod() {
        performAnalysis("instanceLockOnSharedStaticData/StaticLockInsideNotStaticSynchronizedMethod.class");

        assertNumOfSSDBugs(0);
    }

    @Test
    public void findNoSSDBugInClass_LockingOnJavaLangClassObject() {
        performAnalysis("instanceLockOnSharedStaticData/LockingOnJavaLangClassObject.class");

        assertNumOfSSDBugs(0);
    }

    @Test
    public void findNoSSDBugInClass_StaticLockObjectOnStaticSharedData() {
        performAnalysis("instanceLockOnSharedStaticData/StaticLockObjectOnStaticSharedData.class");

        assertNumOfSSDBugs(0);
    }

    private void assertNumOfSSDBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertSSDBug(String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA")
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
