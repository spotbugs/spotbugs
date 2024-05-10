package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindSynchronizationWithAccessibleBackingCollectionTest extends AbstractIntegrationTest {

    // todo remove temp folder
    // todo add testing for exact backing collection
    private final String PUBLIC_BACKING_COLLECTION = "SABC_SYNCHRONIZATION_WITH_PUBLIC_BACKING_COLLECTION";
    private final String ACCESSIBLE_BACKING_COLLECTION = "SABC_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";

    @Test
    public void testBadSynWithAccessiblePublicBackingCollectionInPlace() {
        performAnalysis("accessibleCollectionView/temp/BadSyncWithAccessiblePublicBackingCollectionInPlace.class");

        assertNumOfBugs(26, PUBLIC_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);

        // todo maybe also add lines
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff1",
                "view1");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff1Again",
                "view1");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff2",
                "view2");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff3",
                "view3");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff4",
                "view4");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff5",
                "view5");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff6",
                "view6");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff7",
                "view7");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff8",
                "view8");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff9",
                "view9");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff10",
                "view10");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff11",
                "view11");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff12",
                "view12");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff13",
                "view13");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff14",
                "view14");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff15",
                "view15");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff16",
                "view16");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff17",
                "view17");

        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff18",
                "view18");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff19",
                "view19");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff20",
                "view20");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff21",
                "view21");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff22",
                "view22");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff23",
                "view23");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff24",
                "view24");
        assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff25",
                "view25");

        // todo Report these too
        //         assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff26", "view26");
        //         assertPublicBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithAccessiblePublicBackingCollectionInPlace", "doStuff27", "view27");
    }

    @Test
    public void testBadSynWithBackingCollectionExposedInPlace() {
        performAnalysis("accessibleCollectionView/temp/BadSyncWithBackingCollectionExposedInPlace.class");

        assertNumOfBugs(0, PUBLIC_BACKING_COLLECTION);
        assertNumOfBugs(17, ACCESSIBLE_BACKING_COLLECTION);

        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff1",
                "view1"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff2",
                "view2"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff3",
                "view3"); /* bc: collection1, exp: getCollection1 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff4",
                "view4"); /* bc: collection2, exp: getCollection2 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff5",
                "view5"); /* bc: collection3, exp: setCollection3 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff6",
                "view6"); /* bc: collection4, exp: setCollection4 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff8",
                "view8"); /* bc: view7 -> collection5, exp: getView7 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff9",
                "view9"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff10",
                "view10"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff11",
                "view11"); /* bc: collection6, exp: getCollection6 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff12",
                "view12"); /* bc: collection7, exp: getCollection7 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff13",
                "view13"); /* bc: collection8, exp: setCollection8 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff14",
                "view14"); /* bc: collection9, exp: getCollection9 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff16",
                "view16"); /* bc: view15 -> collection10, exp: getView15 */

        // Complex case
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff20",
                "view20"); /* bc: view19 -> collection12, exp: getCollection12 */
        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff20",
                "view20"); /* bc: view19 -> collection12, exp: getView19 */

        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff18",
                "view18"); /* bc: view17 -> collection11, exp: getCollection11 */
        // not found
        //        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff21", "view21"); /* bc: view21 -> collection13, exp: collection13 */
        //        assertAccessibleBackingCollectionBugExactly("accessibleCollectionView.temp.BadSyncWithBackingCollectionExposedInPlace", "doStuff22", "view22"); /* bc: view22 -> collection14, exp: collection14 */
    }

    @Test
    void experimenting() {
        performAnalysis("accessibleCollectionView/temp/Experimenting.class");

        assertNumOfBugs(0, PUBLIC_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
    }

    //        @Test
    //    public void testBadSynchronizationOnAccessibleBackingCollection() {
    //        performAnalysis("synchronizationOnCollectionView/BadSynchronizationOnCollectionView.class");
    //
    //        assertNumOfBugs(0, PUBLIC_BACKING_COLLECTION);
    //    }

    @Test
    public void testGoodSynchronizationOnCollectionView() {
        performAnalysis("synchronizationOnCollectionView/GoodSynchronizationOnCollectionView.class");

        assertZeroBadSyncBugs();
    }

    private void assertPublicBackingCollectionBugExactly(String clazz, String method, String view) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder =
                new BugInstanceMatcherBuilder()
                        .bugType(PUBLIC_BACKING_COLLECTION)
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

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertZeroBadSyncBugs() {
        assertNumOfBugs(0, PUBLIC_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
    }
}
