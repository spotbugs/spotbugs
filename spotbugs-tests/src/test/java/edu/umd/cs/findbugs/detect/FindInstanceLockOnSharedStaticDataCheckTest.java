package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindInstanceLockOnSharedStaticDataCheckTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA";

    @Test
    void findSSDBugInClass_InstanceLevelLockOnSharedStaticData() {
        performAnalysis("instanceLockOnSharedStaticData/InstanceLevelLockObject.class");
        assertBugTypeCount(BUG_TYPE, 1);

        assertBugInMethodAtLine(BUG_TYPE, "InstanceLevelLockObject", "methodWithBug", 12);
    }

    @Test
    void findSSDBugInClass_InstanceLevelSynchronizedMethod() {
        performAnalysis("instanceLockOnSharedStaticData/InstanceLevelSynchronizedMethod.class");

        assertBugTypeCount(BUG_TYPE, 1);

        assertBugInMethodAtLine(BUG_TYPE, "InstanceLevelSynchronizedMethod", "methodWithBug", 10);
    }

    @Test
    void findNoSSDBugInClass_StaticLockInsideNotStaticSynchronizedMethod() {
        performAnalysis("instanceLockOnSharedStaticData/StaticLockInsideNotStaticSynchronizedMethod.class");

        assertNoBugType(BUG_TYPE);
    }

    @Test
    void findNoSSDBugInClass_LockingOnJavaLangClassObject() {
        performAnalysis("instanceLockOnSharedStaticData/LockingOnJavaLangClassObject.class");

        assertNoBugType(BUG_TYPE);
    }

    @Test
    void findNoSSDBugInClass_StaticLockObjectOnStaticSharedData() {
        performAnalysis("instanceLockOnSharedStaticData/StaticLockObjectOnStaticSharedData.class");

        assertNoBugType(BUG_TYPE);
    }
}
