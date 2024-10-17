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

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 1);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 2);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 3);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 1);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 3);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 1);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 1);

        assertBugInMethodAtField(BAD_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff1",
                "view1"); /* Backed by: view1 -> collection1 */
        assertBugInMethodAtField(BAD_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff2",
                "view2"); /* Backed by: view1 -> collection1 */
        assertBugInMethodAtField(BAD_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff3",
                "view3"); /* Backed by: view1 -> collection1 */
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff1", "view1"); /* public
                                                                                                           lock */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff2", "view2"); /* protected lock */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff3", "view3"); /* package-private lock */

        assertBugInMethodAtField(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff4",
                "view4"); /* Backed by: view4 -> collection2 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff4", "view4"); /* protected lock */
        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.MixedSynchronizationFromParent", "doStuff4",
                "view4"); /* Exposed by getView4 */

        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.MixedSynchronizationFromParent", "doStuff5",
                "view5"); /* Exposed by getView5 in parent */

        assertBugInMethodAtField(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.MixedBadSynchronization", "doStuff6",
                "view6"); /* Backed by: view6 -> collection4, Exposed: getCollection4 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.MixedBadSynchronization", "doStuff6", "view6"); /* Exposed by: getView6() */
    }

    @Test
    void testGoodSynchronization() {
        performAnalysis("synchronizationLocks/GoodSynchronization.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount( BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);
    }
}
