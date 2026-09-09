package edu.umd.cs.findbugs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LaunchAppropriateUITest {

    @AfterEach
    void clearLaunchProperty() {
        System.clearProperty("findbugs.launchUI");
    }

    @Test
    void commandLineUiSelectionStripsFirstArgument() throws Exception {
        LaunchAppropriateUI launcher = new LaunchAppropriateUI(new String[] { "-textui", "-xml", "report.xml" });

        assertEquals(LaunchAppropriateUI.TEXTUI, invokeGetLaunchProperty(launcher));
        assertArrayEquals(new String[] { "-xml", "report.xml" }, getArgs(launcher));
    }

    @Test
    void defaultsToTextUiWhenOutputArgumentsArePresent() throws Exception {
        LaunchAppropriateUI launcher = new LaunchAppropriateUI(new String[] { "input.jar", "-output", "result.xml" });

        assertEquals(LaunchAppropriateUI.TEXTUI, invokeGetLaunchProperty(launcher));
    }

    @Test
    void usesLaunchPropertyWhenPresent() throws Exception {
        System.setProperty("findbugs.launchUI", "help");
        LaunchAppropriateUI launcher = new LaunchAppropriateUI(new String[0]);

        assertEquals(LaunchAppropriateUI.SHOW_HELP, invokeGetLaunchProperty(launcher));
    }

    @Test
    void parsesIntegerLaunchProperty() throws Exception {
        System.setProperty("findbugs.launchUI", "17");
        LaunchAppropriateUI launcher = new LaunchAppropriateUI(new String[0]);

        assertEquals(17, invokeGetLaunchProperty(launcher));
    }

    @Test
    void fallsBackToGui2ForInvalidProperty() throws Exception {
        System.setProperty("findbugs.launchUI", "not-a-ui");
        LaunchAppropriateUI launcher = new LaunchAppropriateUI(new String[0]);

        assertEquals(LaunchAppropriateUI.GUI2, invokeGetLaunchProperty(launcher));
    }

    private static int invokeGetLaunchProperty(LaunchAppropriateUI launcher) throws Exception {
        Method method = LaunchAppropriateUI.class.getDeclaredMethod("getLaunchProperty");
        method.setAccessible(true);
        return (Integer) method.invoke(launcher);
    }

    private static String[] getArgs(LaunchAppropriateUI launcher) throws Exception {
        Field field = LaunchAppropriateUI.class.getDeclaredField("args");
        field.setAccessible(true);
        return (String[]) field.get(launcher);
    }
}
