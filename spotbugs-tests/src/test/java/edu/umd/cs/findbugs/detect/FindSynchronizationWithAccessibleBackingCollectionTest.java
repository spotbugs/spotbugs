package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindSynchronizationWithAccessibleBackingCollectionTest extends AbstractIntegrationTest {
    private static final String BAD_BACKING_COLLECTION = "SABC_BAD_SYNCHRONIZATION_WITH_BACKING_COLLECTION";
    private static final String ACCESSIBLE_BACKING_COLLECTION = "SABC_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";
    private static final String INHERITABLE_BACKING_COLLECTION = "SABC_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION";

    @Test
    void testBadSynWithAccessiblePublicBackingCollectionInPlace() {
        performAnalysis("synchronizationLocks/collectionViews/BadSyncWithAccessiblePublicBackingCollectionInPlace.class");

        assertNumOfBugs(26, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        // todo maybe also add lines
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff1",
                "view1");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff1Again",
                "view1");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff2",
                "view2");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff3",
                "view3");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff4",
                "view4");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff5",
                "view5");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff6",
                "view6");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff7",
                "view7");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff8",
                "view8");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff9",
                "view9");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff10",
                "view10");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff11",
                "view11");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff12",
                "view12");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff13",
                "view13");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff14",
                "view14");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff15",
                "view15");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff16",
                "view16");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff17",
                "view17");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff18",
                "view18");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff19",
                "view19");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff20",
                "view20");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff21",
                "view21");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff22",
                "view22");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff23",
                "view23");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff24",
                "view24");
        assertPublicBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithAccessiblePublicBackingCollectionInPlace",
                "doStuff25",
                "view25");
    }

    @Test
    void testBadSynWithBackingCollectionExposedInPlace() {
        performAnalysis("synchronizationLocks/collectionViews/BadSyncWithBackingCollectionExposedInPlace.class");

        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(17, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff1",
                "view1"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff2",
                "view2"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff3",
                "view3"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff4",
                "view4"); /* bc: collection2, exp: getCollection2 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff5",
                "view5"); /* bc: collection3, exp: setCollection3 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff6",
                "view6"); /* bc: collection4, exp: setCollection4 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff8",
                "view8"); /* bc: view7 -> collection5, exp: getView7 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff9",
                "view9"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff10",
                "view10"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff11",
                "view11"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff12",
                "view12"); /* bc: collection7, exp: getCollection7 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff13",
                "view13"); /* bc: collection8, exp: setCollection8 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff14",
                "view14"); /* bc: collection9, exp: getCollection9 */
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff16",
                "view16"); /* bc: view15 -> collection10, exp: getView15 */

        // Complex case
        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff20",
                "view20", 2); /* bc: view19 -> collection12, exp: getCollection12 */

        assertAccessibleBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithBackingCollectionExposedInPlace", "doStuff18",
                "view18"); /* bc: view17 -> collection11, exp: getCollection11 */
    }


    @Test
    void testBadSyncWithInheritableBackingCollection() {
        performAnalysis("synchronizationLocks/collectionViews/BadSyncWithPotentiallyInheritedProtectedBackingCollections.class",
                "synchronizationLocks/collectionViews/BadSyncWithExposedPackagePrivateBackingCollections.class");

        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(10, INHERITABLE_BACKING_COLLECTION);

        /* Protected bugs */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.BadSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff1",
                "view1"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.BadSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff2",
                "view2"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.BadSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff3",
                "view3"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.BadSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff4",
                "view4"); /* Backed by: view4 -> collection2 */
        assertInheritableBackingCollectionBugExactly(
                "synchronizationLocks.collectionViews.BadSyncWithPotentiallyInheritedProtectedBackingCollections", "doStuff5",
                "view5"); /* Backed by: view5 -> collection3 */

        /* Package private bugs */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithExposedPackagePrivateBackingCollections",
                "doStuff1", "view1"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithExposedPackagePrivateBackingCollections",
                "doStuff2", "view2"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithExposedPackagePrivateBackingCollections",
                "doStuff3", "view3"); /* Backed by: view1 -> collection1 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithExposedPackagePrivateBackingCollections",
                "doStuff4", "view4"); /* Backed by: view4 -> collection2 */
        assertInheritableBackingCollectionBugExactly("synchronizationLocks.collectionViews.BadSyncWithExposedPackagePrivateBackingCollections",
                "doStuff5", "view5"); /* Backed by: view5 -> collection3 */
    }

    @Test
    public void testGoodSynchronizationOnCollectionView() {
        performAnalysis("synchronizationLocks/collectionViews/GoodSynchronizationOnCollectionView.class");

        assertZeroBadSyncBugs();
    }

    private void assertPublicBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder =
                new BugInstanceMatcherBuilder()
                        .bugType(BAD_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder =
                new BugInstanceMatcherBuilder()
                        .bugType(ACCESSIBLE_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleBackingCollectionBugExactly(String clazz, String method, String view, int numberOfBugs) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder =
                new BugInstanceMatcherBuilder()
                        .bugType(ACCESSIBLE_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), containsExactly(2, bugInstance));
    }

    private void assertInheritableBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder =
                new BugInstanceMatcherBuilder()
                        .bugType(INHERITABLE_BACKING_COLLECTION)
                        .inClass(clazz)
                        .inMethod(method)
                        .atField(view);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

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
