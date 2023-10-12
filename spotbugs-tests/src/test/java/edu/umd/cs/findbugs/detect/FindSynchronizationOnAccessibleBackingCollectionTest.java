package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindSynchronizationOnAccessibleBackingCollectionTest extends AbstractIntegrationTest {

    @Test
    public void testBadSynchronizationOnAccessibleBackingCollection() {
        performAnalysis("synchronizationOnCollectionView/BadSynchronizationOnCollectionView.class");

        assertNrOfBugs(0);
    }

    @Test
    public void testGoodSynchronizationOnCollectionView() {
        performAnalysis("synchronizationOnCollectionView/GoodSynchronizationOnCollectionView.class");

        assertNrOfBugs(0);
    }

    private void assertNrOfBugs(int number) {
        assertEquals(getBugCollection().getCollection().size(), number);
    }
}
