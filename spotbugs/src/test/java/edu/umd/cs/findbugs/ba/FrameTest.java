package edu.umd.cs.findbugs.ba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class FrameTest {

    @Test
    public void testToString() {
        Frame<String> frame = new Frame<String>(1) {
            @Override
            public String getValue(int n) {
                return "value";
            }
        };
        assertThat(frame.toString(), is(equalTo("[value]")));
    }
}
