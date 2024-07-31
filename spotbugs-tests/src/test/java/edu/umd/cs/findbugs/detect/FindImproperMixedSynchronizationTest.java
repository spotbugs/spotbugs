package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindImproperMixedSynchronizationTest extends AbstractIntegrationTest {
    private static final String METHOD_BUG = "US_UNSAFE_METHOD_SYNCHRONIZATION";
    private static final String STATIC_METHOD_BUG = "US_UNSAFE_STATIC_METHOD_SYNCHRONIZATION";
    private static final String OBJECT_BUG = "US_UNSAFE_OBJECT_SYNCHRONIZATION";
    private static final String ACCESSIBLE_OBJECT_BUG = "US_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION";
    private static final String INHERITABLE_OBJECT_BUG = "US_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION";
    private static final String EXPOSED_LOCK_OBJECT_BUG = "US_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION";
    private static final String BAD_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_BACKING_COLLECTION";
    private static final String ACCESSIBLE_BACKING_COLLECTION =
            "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";
    private static final String INHERITABLE_BACKING_COLLECTION =
            "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION";


    @Test
    void testMixedSynchronizations() {
        performAnalysis("synchronizationLocks/MixedBadSynchronization.class", "synchronizationLocks" +
                "/MixedSynchronizationFromParent.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(1, OBJECT_BUG);
        assertNumOfBugs(2, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(3, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(1, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(3, BAD_BACKING_COLLECTION);
        assertNumOfBugs(1, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(1, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff1",
                "view1"); /* Backed by: view1 -> collection1 */
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff2",
                "view2"); /* Backed by: view1 -> collection1 */
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff3",
                "view3"); /* Backed by: view1 -> collection1 */
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff1", "view1"); /* public
                                                                                                           lock */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff2", "view2"); /* protected lock */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff3", "view3"); /* package-private lock */

        assertBugExactly(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff4",
                "view4"); /* Backed by: view4 -> collection2 */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff4", "view4"); /* protected lock */
        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.MixedSynchronizationFromParent", "doStuff4",
                "view4"); /* Exposed by getView4 */

        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.MixedSynchronizationFromParent", "doStuff5",
                "view5"); /* Exposed by getView5 in parent */

        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff6",
                "view6"); /* Backed by: view6 -> collection4, Exposed: getCollection4 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff6", "view6"); /* Exposed by: getView6() */
    }

    @Test
    void testGoodSynchronization() {
        performAnalysis("synchronizationLocks/GoodSynchronization.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);
    }


    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertBugExactly(String bugtype, String clazz, String method, String field) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstance));
    }
}
