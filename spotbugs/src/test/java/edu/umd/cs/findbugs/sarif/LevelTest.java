package edu.umd.cs.findbugs.sarif;

import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LevelTest {
    @Test
    public void testToJsonString() {
        JSONObject jsonObject = new JSONObject().put("level", Level.WARNING);
        assertThat(jsonObject.toString(), is("{\"level\":\"warning\"}"));
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
}
