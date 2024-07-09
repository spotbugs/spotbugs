package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindImproperMixedSynchronizationTest extends AbstractIntegrationTest {
    private static final String OBJECT_BUG = "US_UNSAFE_OBJECT_SYNCHRONIZATION";
    private static final String ACCESSIBLE_OBJECT_BUG = "US_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION";
    private static final String INHERITABLE_OBJECT_BUG = "US_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION";
    private static final String EXPOSED_LOCK_OBJECT_BUG = "US_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION";
    private final String BAD_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_BACKING_COLLECTION";
    private final String ACCESSIBLE_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";
    private final String INHERITABLE_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION";


    @Test
    void testMixedSynchronizations() {
        performAnalysis("synchronizationLocks/MixedBadSynchronization.class",
                "synchronizationLocks/MixedSynchronizationFromParent.class");

        assertNumOfBugs(1, OBJECT_BUG);
        assertNumOfBugs(2, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(3, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(1, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(3, BAD_BACKING_COLLECTION);
        assertNumOfBugs(1, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(1, INHERITABLE_BACKING_COLLECTION);

        assertBadBackingCollectionBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff1",
                "view1"); /* Backed by: view1 -> collection1 */
        assertBadBackingCollectionBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff2",
                "view2"); /* Backed by: view1 -> collection1 */
        assertBadBackingCollectionBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff3",
                "view3"); /* Backed by: view1 -> collection1 */
        assertObjectBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff1", "view1"); /* public lock */
        assertInheritedObjectBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff2", "view2"); /* protected lock */
        assertInheritedObjectBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff3", "view3"); /* package-private lock */

        assertInheritableBackingCollectionBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff4",
                "view4"); /* Backed by: view4 -> collection2 */
        assertInheritedObjectBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff4", "view4"); /* protected lock */
        assertExposingObjectBugExactly("synchronizationLocks.MixedSynchronizationFromParent", "doStuff4", "view4"); /* Exposed by getView4 */

        assertAccessibleObjectBugExactly("synchronizationLocks.MixedSynchronizationFromParent", "doStuff5",
                "view5"); /* Exposed by getView5 in parent */

        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff6",
                "view6"); /* Backed by: view6 -> collection4, Exposed: getCollection4 */
        assertAccessibleObjectBugExactly("synchronizationLocks.MixedBadSynchronization", "doStuff6", "view6"); /* Exposed by: getView6() */
    }

    @Test
    void testGoodSynchronization() {
        performAnalysis("synchronizationLocks/GoodSynchronization.class");

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

    private void assertObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(ACCESSIBLE_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertInheritedObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(INHERITABLE_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertExposingObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(EXPOSED_LOCK_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertBadBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(BAD_BACKING_COLLECTION)
                .inClass(clazz)
                .inMethod(method)
                .atField(view)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(ACCESSIBLE_BACKING_COLLECTION)
                .inClass(clazz)
                .inMethod(method)
                .atField(view)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertInheritableBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(INHERITABLE_BACKING_COLLECTION)
                .inClass(clazz)
                .inMethod(method)
                .atField(view)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }
}
