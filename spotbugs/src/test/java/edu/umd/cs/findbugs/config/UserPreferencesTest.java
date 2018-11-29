package edu.umd.cs.findbugs.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class UserPreferencesTest {

    @Test
    public void testClone() {
        UserPreferences sut = UserPreferences.createDefaultUserPreferences();

        UserPreferences clone = sut.clone();

        Assert.assertEquals(sut, clone);
        Assert.assertEquals(sut.getClass(), clone.getClass());
    }

    @Test
    public void testReadEffortNoPrefix() throws IOException {
        String testPrefsString = "effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assert.assertEquals(UserPreferences.EFFORT_MAX, userPrefs.getEffort());
    }

    @Test
    public void testReadEffortSpotBugsPrefix() throws IOException {
        String testPrefsString = "/instance/com.github.spotbugs.plugin.eclipse/effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assert.assertEquals(UserPreferences.EFFORT_MAX, userPrefs.getEffort());
    }

    @Test
    public void testReadEffortFindBugsPrefix() throws IOException {
        String testPrefsString = "/instance/edu.umd.cs.findbugs.plugin.eclipse/effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assert.assertEquals(UserPreferences.EFFORT_MAX, userPrefs.getEffort());
    }

    @Test
    public void testReadEffortUnknownPrefix() throws IOException {
        String testPrefsString = "/instance/test.test.test.test.plugin.eclipse/effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assert.assertEquals(UserPreferences.EFFORT_DEFAULT, userPrefs.getEffort());
    }
}
