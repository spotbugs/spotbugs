package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindImproperSynchronizationWithAccessibleBackingCollectionsTest extends AbstractIntegrationTest {
    private static final String METHOD_BUG = "US_UNSAFE_METHOD_SYNCHRONIZATION";
    private static final String STATIC_METHOD_BUG = "US_UNSAFE_STATIC_METHOD_SYNCHRONIZATION";
    private static final String OBJECT_BUG = "US_UNSAFE_OBJECT_SYNCHRONIZATION";
    private static final String ACCESSIBLE_OBJECT_BUG = "US_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION";
    private static final String INHERITABLE_OBJECT_BUG = "US_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION";
    private static final String EXPOSED_LOCK_OBJECT_BUG = "US_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION";
    private static final String BAD_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_BACKING_COLLECTION";
    private static final String ACCESSIBLE_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";
    private static final String INHERITABLE_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION";

    @Test
    void testUnsafeSynWithAccessiblePublicBackingCollectionInPlace() {
        performAnalysis("synchronizationLocks/collectionViews/UnsafeSyncWithAccessiblePublicBackingCollectionInPlace.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(26, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff1",
                "view1");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff1Again",
                "view1");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff2",
                "view2");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff3",
                "view3");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff4",
                "view4");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff5",
                "view5");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff6",
                "view6");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff7",
                "view7");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff8",
                "view8");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff9",
                "view9");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff10",
                "view10");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff11",
                "view11");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff12",
                "view12");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff13",
                "view13");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff14",
                "view14");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff15",
                "view15");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff16",
                "view16");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff17",
                "view17");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff18",
                "view18");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff19",
                "view19");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff20",
                "view20");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff21",
                "view21");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff22",
                "view22");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff23",
                "view23");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff24",
                "view24");
        assertBugExactly(BAD_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff25",
                "view25");
    }

    @Test
    void testUnsafeSynWithBackingCollectionExposedInPlace() {
        performAnalysis("synchronizationLocks/collectionViews/UnsafeSyncWithBackingCollectionExposedInPlace.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(17, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff1",
                "view1"); /* bc: collection1, exp: getCollection1 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff2",
                "view2"); /* bc: collection1, exp: getCollection1 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff3",
                "view3"); /* bc: collection1, exp: getCollection1 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff4",
                "view4"); /* bc: collection2, exp: getCollection2 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff5",
                "view5"); /* bc: collection3, exp: setCollection3 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff6",
                "view6"); /* bc: collection4, exp: setCollection4 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff8",
                "view8"); /* bc: view7 -> collection5, exp: getView7 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff9",
                "view9"); /* bc: collection6, exp: getCollection6 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff10",
                "view10"); /* bc: collection6, exp: getCollection6 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff11",
                "view11"); /* bc: collection6, exp: getCollection6 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff12",
                "view12"); /* bc: collection7, exp: getCollection7 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff13",
                "view13"); /* bc: collection8, exp: setCollection8 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff14",
                "view14"); /* bc: collection9, exp: getCollection9 */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff16",
                "view16"); /* bc: view15 -> collection10, exp: getView15 */

        /* Complex case */
        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff20",
                "view20", 2); /* bc: view19 -> collection12, exp: getCollection12 */

        assertBugExactly(ACCESSIBLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace",
                "doStuff18",
                "view18"); /* bc: view17 -> collection11, exp: getCollection11 */
    }


    @Test
    void testUnsafeSyncWithInheritableBackingCollection() {
        performAnalysis("synchronizationLocks/collectionViews/UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections.class",
                "synchronizationLocks/collectionViews/UnsafeSyncWithExposedPackagePrivateBackingCollections.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(10, INHERITABLE_BACKING_COLLECTION);

        /* Protected bugs */
        assertBugExactly(
                INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections",
                "doStuff1",
                "view1"); /* Backed by: view1 -> collection1 */
        assertBugExactly(
                INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections",
                "doStuff2",
                "view2"); /* Backed by: view1 -> collection1 */
        assertBugExactly(
                INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections",
                "doStuff3",
                "view3"); /* Backed by: view1 -> collection1 */
        assertBugExactly(
                INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections",
                "doStuff4",
                "view4"); /* Backed by: view4 -> collection2 */
        assertBugExactly(
                INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections",
                "doStuff5",
                "view5"); /* Backed by: view5 -> collection3 */

        /* Package private bugs */
        assertBugExactly(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff1", "view1"); /* Backed by: view1 -> collection1 */
        assertBugExactly(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff2", "view2"); /* Backed by: view1 -> collection1 */
        assertBugExactly(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff3", "view3"); /* Backed by: view1 -> collection1 */
        assertBugExactly(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff4", "view4"); /* Backed by: view4 -> collection2 */
        assertBugExactly(INHERITABLE_BACKING_COLLECTION, "synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff5", "view5"); /* Backed by: view5 -> collection3 */
    }

    @Test
    public void testSafeSynchronizationOnCollectionView() {
        performAnalysis("synchronizationLocks/collectionViews/SafeSynchronizationOnCollectionView.class");

        assertZeroBadSyncBugs();
    }

    private void assertBugExactly(String bugType, String clazz, String method, String view) {
        BugInstanceMatcher bugInstance =
                new BugInstanceMatcherBuilder()
                        .bugType(bugType)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view)
                        .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertBugExactly(String bugType, String clazz, String method, String view, int numberOfBugs) {
        BugInstanceMatcher bugInstance =
                new BugInstanceMatcherBuilder()
                        .bugType(bugType)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view)
                        .build();

        assertThat(getBugCollection(), containsExactly(numberOfBugs, bugInstance));
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertZeroBadSyncBugs() {
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
}
