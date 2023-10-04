package edu.umd.cs.findbugs.sarif;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.umd.cs.findbugs.cwe.WeaknessSeverity;

public class LevelTest {
    @Test
    public void testToJsonString() {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("level", Level.WARNING.toJsonString());
        assertThat(gson.toJson(jsonObject), is("{\"level\":\"warning\"}"));
    }

    @Test
    public void testMapHighestRankToError() {
        assertThat(Level.fromBugRank(1), is(Level.ERROR));
    }

    @Test
    public void testMapHighRankToError() {
        assertThat(Level.fromBugRank(9), is(Level.ERROR));
    }

    @Test
    public void testMapLowRankToWarning() {
        assertThat(Level.fromBugRank(14), is(Level.WARNING));
    }

    @Test
    public void testMapLowestRankToNote() {
        assertThat(Level.fromBugRank(20), is(Level.NOTE));
    }

    @Test
    public void testMapWeaknessSeverityLevelHighToError() {
        assertThat(Level.fromWeaknessSeverity(WeaknessSeverity.HIGH), is(Level.ERROR));
    }

    @Test
    public void testMapWeaknessSeverityLevelMediumToWarning() {
        assertThat(Level.fromWeaknessSeverity(WeaknessSeverity.MEDIUM), is(Level.WARNING));
    }

    @Test
    public void testMapWeaknessSeverityLevelLowToNote() {
        assertThat(Level.fromWeaknessSeverity(WeaknessSeverity.LOW), is(Level.NOTE));
    }

    @Test
    public void testMapWeaknessSeverityLevelNoneToNone() {
        assertThat(Level.fromWeaknessSeverity(WeaknessSeverity.NONE), is(Level.NONE));
    }
}
