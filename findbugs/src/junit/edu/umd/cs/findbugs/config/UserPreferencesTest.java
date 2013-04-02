package edu.umd.cs.findbugs.config;

import junit.framework.Assert;
import junit.framework.TestCase;

public class UserPreferencesTest extends TestCase {
    UserPreferences prefs;

    @Override
    protected void setUp() throws Exception {
        prefs = UserPreferences.createDefaultUserPreferences();
    }

    public void testClone() {
        UserPreferences clone = prefs.clone();

        Assert.assertEquals(prefs, clone);
        Assert.assertEquals(prefs.getClass(), clone.getClass());
    }
}
