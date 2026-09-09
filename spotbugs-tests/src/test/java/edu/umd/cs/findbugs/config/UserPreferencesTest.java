package edu.umd.cs.findbugs.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserPreferencesTest {

    @Test
    void testClone() {
        UserPreferences sut = UserPreferences.createDefaultUserPreferences();

        UserPreferences clone = sut.clone();

        Assertions.assertEquals(sut, clone);
        Assertions.assertEquals(sut.getClass(), clone.getClass());
    }

    @Test
    void testReadEffortNoPrefix() throws IOException {
        String testPrefsString = "effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assertions.assertEquals(UserPreferences.EFFORT_MAX, userPrefs.getEffort());
    }

    @Test
    void testReadEffortSpotBugsPrefix() throws IOException {
        String testPrefsString = "/instance/com.github.spotbugs.plugin.eclipse/effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assertions.assertEquals(UserPreferences.EFFORT_MAX, userPrefs.getEffort());
    }

    @Test
    void testReadEffortFindBugsPrefix() throws IOException {
        String testPrefsString = "/instance/edu.umd.cs.findbugs.plugin.eclipse/effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assertions.assertEquals(UserPreferences.EFFORT_MAX, userPrefs.getEffort());
    }

    @Test
    void testReadEffortUnknownPrefix() throws IOException {
        String testPrefsString = "/instance/test.test.test.test.plugin.eclipse/effort=max";

        UserPreferences userPrefs = UserPreferences.createDefaultUserPreferences();

        userPrefs.read(new ByteArrayInputStream(testPrefsString.getBytes(StandardCharsets.ISO_8859_1)));
        Assertions.assertEquals(UserPreferences.EFFORT_DEFAULT, userPrefs.getEffort());
    }
}
