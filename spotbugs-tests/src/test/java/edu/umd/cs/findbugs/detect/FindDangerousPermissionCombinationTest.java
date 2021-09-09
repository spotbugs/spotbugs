package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindDangerousPermissionCombinationTest extends AbstractIntegrationTest {
    @Test
    public void testReflectPermissionSuperAccessChecks() {
        performAnalysis("dangerousPermissionCombination/ReflectPermissionSuppressAccessChecks.class");

        assertNumOfDPCBugs(1);

        assertDPCBug("ReflectPermissionSuppressAccessChecks", 12);
    }

    @Test
    public void testReflectPermissionNewProxyInPackage() {
        performAnalysis("dangerousPermissionCombination/ReflectPermissionNewProxyInPackage.class");

        assertNumOfDPCBugs(0);
    }

    @Test
    public void testRuntimePermissionCreateClassLoader() {
        performAnalysis("dangerousPermissionCombination/RuntimePermissionCreateClassLoader.class");

        assertNumOfDPCBugs(1);

        assertDPCBug("RuntimePermissionCreateClassLoader", 11);
    }

    @Test
    public void testRuntimePermissionGetClassLoader() {
        performAnalysis("dangerousPermissionCombination/RuntimePermissionGetClassLoader.class");

        assertNumOfDPCBugs(0);
    }

    private void assertNumOfDPCBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DPC_DANGEROUS_PERMISSION_COMBINATION").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertDPCBug(String klass, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("DPC_DANGEROUS_PERMISSION_COMBINATION")
                .inClass(klass)
                .inMethod("getPermissions")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
