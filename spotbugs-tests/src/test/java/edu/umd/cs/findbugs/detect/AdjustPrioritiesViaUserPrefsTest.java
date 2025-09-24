package edu.umd.cs.findbugs.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.config.UserPreferences;

class AdjustPrioritiesViaUserPrefsTest extends AbstractIntegrationTest {

    @AfterEach
    public void afterEach() {
        FindBugs2.resetPriorityAdjustments();
    }

    @Test
    void testDefaultPreferencesShouldNotSetAnyAdjustments() {
        try (FindBugs2 engine = new FindBugs2()) {
            engine.setUserPreferences(UserPreferences.createDefaultUserPreferences());
            DetectorFactoryCollection.instance().factoryIterator().forEachRemaining(f -> {
                assertEquals(0, f.getPriorityAdjustment());
                f.getReportedBugPatterns().forEach(bp -> assertEquals(0, bp.getPriorityAdjustment()));
            });
        }
    }

    @Test
    void testDetectorByFQCN() {
        try (FindBugs2 engine = new FindBugs2()) {
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setAdjustPriority(Map.of("edu.umd.cs.findbugs.detect.UnreadFields", "raise"));
            engine.setUserPreferences(prefs);

            boolean found = false;
            Iterator<DetectorFactory> factoryIterator = DetectorFactoryCollection.instance().factoryIterator();
            while (factoryIterator.hasNext()) {
                DetectorFactory f = factoryIterator.next();
                if (f.getFullName().equals("edu.umd.cs.findbugs.detect.UnreadFields")) {
                    assertEquals(-1, f.getPriorityAdjustment());
                    found = true;
                } else {
                    assertEquals(0, f.getPriorityAdjustment());
                }
                f.getReportedBugPatterns().forEach(bp -> assertEquals(0, bp.getPriorityAdjustment()));
            }
            assertTrue(found, "detector not found - adapt test?");
        }
    }

    @Test
    void testDetectorBySimpleName() {
        try (FindBugs2 engine = new FindBugs2()) {
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setAdjustPriority(Map.of("UnreadFields", "lower"));
            engine.setUserPreferences(prefs);

            boolean found = false;
            Iterator<DetectorFactory> factoryIterator = DetectorFactoryCollection.instance().factoryIterator();
            while (factoryIterator.hasNext()) {
                DetectorFactory f = factoryIterator.next();
                if (f.getFullName().endsWith(".UnreadFields")) {
                    assertEquals(1, f.getPriorityAdjustment());
                    found = true;
                } else {
                    assertEquals(0, f.getPriorityAdjustment());
                }
                f.getReportedBugPatterns().forEach(bp -> assertEquals(0, bp.getPriorityAdjustment()));
            }
            assertTrue(found, "detector not found - adapt test?");
        }
    }

    @Test
    void testBugPattern() {
        try (FindBugs2 engine = new FindBugs2()) {
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setAdjustPriority(Map.of("RC_REF_COMPARISON", "suppress"));
            engine.setUserPreferences(prefs);

            boolean found = false;
            Iterator<DetectorFactory> factoryIterator = DetectorFactoryCollection.instance().factoryIterator();
            while (factoryIterator.hasNext()) {
                DetectorFactory f = factoryIterator.next();
                assertEquals(0, f.getPriorityAdjustment());
                for (BugPattern bp : f.getReportedBugPatterns()) {
                    if (bp.getType().equals("RC_REF_COMPARISON")) {
                        assertEquals(100, bp.getPriorityAdjustment());
                        found = true;
                    } else {
                        assertEquals(0, bp.getPriorityAdjustment());
                    }
                }
            }
            assertTrue(found, "bug pattern not found - adapt test?");
        }
    }

    @Test
    void testMultiple() {
        try (FindBugs2 engine = new FindBugs2()) {
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setAdjustPriority(Map.of("RC_REF_COMPARISON", "suppress", "UnreadFields", "lower"));
            engine.setUserPreferences(prefs);

            boolean bugPatternFound = false;
            boolean detectorFound = false;
            Iterator<DetectorFactory> factoryIterator = DetectorFactoryCollection.instance().factoryIterator();
            while (factoryIterator.hasNext()) {
                DetectorFactory f = factoryIterator.next();
                if (f.getFullName().endsWith(".UnreadFields")) {
                    assertEquals(1, f.getPriorityAdjustment());
                    detectorFound = true;
                } else {
                    assertEquals(0, f.getPriorityAdjustment());
                }
                for (BugPattern bp : f.getReportedBugPatterns()) {
                    if (bp.getType().equals("RC_REF_COMPARISON")) {
                        assertEquals(100, bp.getPriorityAdjustment());
                        bugPatternFound = true;
                    } else {
                        assertEquals(0, bp.getPriorityAdjustment());
                    }
                }
            }
            assertTrue(bugPatternFound, "bug pattern not found - adapt test?");
            assertTrue(detectorFound, "detector not found - adapt test?");
        }
    }

    @Test
    void testBadValue() {
        try (FindBugs2 engine = new FindBugs2()) {
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setAdjustPriority(Map.of("RC_REF_COMPARISON", "something"));
            assertThrows(IllegalArgumentException.class, () -> {
                engine.setUserPreferences(prefs);
            });
        }
    }

    @Test
    void testBadKey() {
        try (FindBugs2 engine = new FindBugs2()) {
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setAdjustPriority(Map.of("something", "suppress"));
            assertThrows(IllegalArgumentException.class, () -> {
                engine.setUserPreferences(prefs);
            });
        }
    }

}
