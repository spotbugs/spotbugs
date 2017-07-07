package edu.umd.cs.findbugs.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UserPreferencesTest {

    UserPreferences prefs;

    @Before
    public void setUp() {
        prefs = UserPreferences.createDefaultUserPreferences();
    }

    @Test
    public void testClone() {
        UserPreferences clone = prefs.clone();

        Assert.assertEquals(prefs, clone);
        Assert.assertEquals(prefs.getClass(), clone.getClass());
    }
}
