/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Priorities;

class ProjectFilterSettingsTest {

    ProjectFilterSettings plain;

    ProjectFilterSettings otherPlain;

    ProjectFilterSettings changed;

    ProjectFilterSettings changed2;

    ProjectFilterSettings changed3;

    ProjectFilterSettings changed4;

    @BeforeEach
    void setUp() {
        plain = ProjectFilterSettings.createDefault();

        otherPlain = ProjectFilterSettings.createDefault();

        changed = ProjectFilterSettings.createDefault();
        changed.setMinPriority("High");

        changed2 = ProjectFilterSettings.createDefault();
        changed2.removeCategory("MALICIOUS_CODE");

        changed3 = ProjectFilterSettings.createDefault();
        changed3.addCategory("FAKE_CATEGORY");

        changed4 = ProjectFilterSettings.createDefault();
        changed4.setMinPriority("High");
        changed4.removeCategory("MALICIOUS_CODE");
        changed4.addCategory("FAKE_CATEGORY");

    }

    @Test
    void testPlainPrio() {
        Assertions.assertTrue(plain.getMinPriority().equals(ProjectFilterSettings.DEFAULT_PRIORITY));
    }

    @Test
    void testPlainCategories() {
        int count = 0;
        for (String category : DetectorFactoryCollection.instance().getBugCategories()) {
            if (!category.equals("NOISE")) {
                Assertions.assertTrue(plain.containsCategory(category));
                ++count;
            }
        }
        Assertions.assertEquals(count, plain.getActiveCategorySet().size());
    }

    @Test
    void testAddCategory() {
        Assertions.assertTrue(plain.containsCategory("FAKE_CATEGORY")); // unknown
        // categories
        // should be
        // unhidden
        // by
        // default
        plain.addCategory("FAKE_CATEGORY");
        Assertions.assertTrue(plain.containsCategory("FAKE_CATEGORY"));
    }

    @Test
    void testRemoveCategory() {
        Assertions.assertTrue(plain.containsCategory("MALICIOUS_CODE"));
        plain.removeCategory("MALICIOUS_CODE");
        Assertions.assertFalse(plain.containsCategory("MALICIOUS_CODE"));
    }

    @Test
    void testSetMinPriority() {
        plain.setMinPriority("High");
        Assertions.assertTrue(plain.getMinPriority().equals("High"));
        Assertions.assertTrue(plain.getMinPriorityAsInt() == Priorities.HIGH_PRIORITY);
        plain.setMinPriority("Medium");
        Assertions.assertTrue(plain.getMinPriority().equals("Medium"));
        Assertions.assertTrue(plain.getMinPriorityAsInt() == Priorities.NORMAL_PRIORITY);
        plain.setMinPriority("Low");
        Assertions.assertTrue(plain.getMinPriority().equals("Low"));
        Assertions.assertTrue(plain.getMinPriorityAsInt() == Priorities.LOW_PRIORITY);
    }

    @Test
    void testEquals() {
        Assertions.assertEquals(plain, otherPlain);

        Assertions.assertFalse(plain.equals(changed));
        Assertions.assertFalse(changed.equals(plain));

        Assertions.assertFalse(plain.equals(changed2));
        Assertions.assertFalse(changed2.equals(plain));

        // The activeBugCategorySet doesn't matter for equals(), only
        // the hiddenBugCategorySet does (along with minPriority and
        // displayFalseWarnings) so 'plain' and 'changed3' should test equal.
        Assertions.assertTrue(plain.equals(changed3));
        Assertions.assertTrue(changed3.equals(plain));

        Assertions.assertFalse(plain.equals(changed4));
        Assertions.assertFalse(changed4.equals(plain));
    }

    @Test
    void testEncodeDecode() {
        ProjectFilterSettings copyOfPlain = ProjectFilterSettings.fromEncodedString(plain.toEncodedString());
        ProjectFilterSettings.hiddenFromEncodedString(copyOfPlain, plain.hiddenToEncodedString());
        Assertions.assertEquals(plain, copyOfPlain);

        ProjectFilterSettings copyOfChanged4 = ProjectFilterSettings.fromEncodedString(changed4.toEncodedString());
        ProjectFilterSettings.hiddenFromEncodedString(copyOfChanged4, changed4.hiddenToEncodedString());
        Assertions.assertEquals(changed4, copyOfChanged4);
    }

    @Test
    void testDisplayFalseWarnings() {
        Assertions.assertEquals(plain, otherPlain);

        Assertions.assertFalse(plain.displayFalseWarnings());
        plain.setDisplayFalseWarnings(true);

        Assertions.assertFalse(plain.equals(otherPlain));

        ProjectFilterSettings copyOfPlain = ProjectFilterSettings.fromEncodedString(plain.toEncodedString());

        Assertions.assertTrue(copyOfPlain.displayFalseWarnings());
        Assertions.assertEquals(copyOfPlain, plain);
        Assertions.assertEquals(plain, copyOfPlain);
    }
}
