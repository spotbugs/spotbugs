package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindImproperSynchronizationWithAccessibleBackingCollectionsTest extends AbstractIntegrationTest {
    private static final String BAD_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_BACKING_COLLECTION";
    private static final String ACCESSIBLE_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";
    private static final String INHERITABLE_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION";

    @Test
    void testUnsafeSynWithAccessiblePublicBackingCollectionInPlace() {
        performAnalysis("synchronizationLocks/collectionViews/UnsafeSyncWithAccessiblePublicBackingCollectionInPlace.class");

        assertNumOfBugs(26, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff1",
                "view1");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff1Again",
                "view1");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff2",
                "view2");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff3",
                "view3");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff4",
                "view4");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff5",
                "view5");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff6",
                "view6");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff7",
                "view7");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff8",
                "view8");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff9",
                "view9");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff10",
                "view10");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff11",
                "view11");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff12",
                "view12");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff13",
                "view13");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff14",
                "view14");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff15",
                "view15");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff16",
                "view16");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff17",
                "view17");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff18",
                "view18");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff19",
                "view19");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff20",
                "view20");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff21",
                "view21");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff22",
                "view22");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff23",
                "view23");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff24",
                "view24");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff25",
                "view25");
    }

    @Test
    void testUnsafeSynWithBackingCollectionExposedInPlace() {
        performAnalysis("synchronizationLocks/collectionViews/UnsafeSyncWithBackingCollectionExposedInPlace.class");

        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(17, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff1",
                "view1"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff2",
                "view2"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff3",
                "view3"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff4",
                "view4"); /* bc: collection2, exp: getCollection2 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff5",
                "view5"); /* bc: collection3, exp: setCollection3 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff6",
                "view6"); /* bc: collection4, exp: setCollection4 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff8",
                "view8"); /* bc: view7 -> collection5, exp: getView7 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff9",
                "view9"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff10",
                "view10"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff11",
                "view11"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff12",
                "view12"); /* bc: collection7, exp: getCollection7 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff13",
                "view13"); /* bc: collection8, exp: setCollection8 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff14",
                "view14"); /* bc: collection9, exp: getCollection9 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff16",
                "view16"); /* bc: view15 -> collection10, exp: getView15 */

        /* Complex case */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff20",
                "view20", 2); /* bc: view19 -> collection12, exp: getCollection12 */

        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithBackingCollectionExposedInPlace", "doStuff18",
                "view18"); /* bc: view17 -> collection11, exp: getCollection11 */
    }


    @Test
    void testUnsafeSyncWithInheritableBackingCollection() {
        performAnalysis("synchronizationLocks/collectionViews/UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections.class",
                "synchronizationLocks/collectionViews/UnsafeSyncWithExposedPackagePrivateBackingCollections.class");

        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(10, INHERITABLE_BACKING_COLLECTION);

        /* Protected bugs */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff1",
                "view1"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff2",
                "view2"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff3",
                "view3"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff4",
                "view4"); /* Backed by: view4 -> collection2 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.UnsafeSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff5",
                "view5"); /* Backed by: view5 -> collection3 */

        /* Package private bugs */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff1", "view1"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff2", "view2"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff3", "view3"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff4", "view4"); /* Backed by: view4 -> collection2 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.UnsafeSyncWithExposedPackagePrivateBackingCollections",
                "doStuff5", "view5"); /* Backed by: view5 -> collection3 */
    }

    @Test
    public void testSafeSynchronizationOnCollectionView() {
        performAnalysis("synchronizationLocks/collectionViews/SafeSynchronizationOnCollectionView.class");

        assertZeroBadSyncBugs();
    }

    private void assertPublicBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcher bugInstance =
                new BugInstanceMatcherBuilder()
                        .bugType(BAD_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view)
                        .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcher bugInstance =
                new BugInstanceMatcherBuilder()
                        .bugType(ACCESSIBLE_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view)
                        .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleBackingCollectionBugExactly(String clazz, String method, String view, int numberOfBugs) {
        BugInstanceMatcher bugInstance =
                new BugInstanceMatcherBuilder()
                        .bugType(ACCESSIBLE_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view)
                        .build();

        assertThat(getBugCollection(), containsExactly(numberOfBugs, bugInstance));
    }

    private void assertInheritableBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcher bugInstance =
                new BugInstanceMatcherBuilder()
                        .bugType(INHERITABLE_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view)
                        .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertZeroBadSyncBugs() {
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);
    }
}
