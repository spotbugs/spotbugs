package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Priorities;
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
    public void testMapLowestPriorityToNote() {
        assertThat(Level.fromPriority(Priorities.IGNORE_PRIORITY), is(Level.NOTE));
    }
}
