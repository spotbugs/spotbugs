package edu.umd.cs.findbugs.ba;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class FrameTest {

    @Test
    void testToString() {
        Frame<String> frame = new Frame<String>(1) {
            @Override
            public String getValue(int n) {
                return "value";
            }
        };
        assertThat(frame.toString(), is(equalTo("[value]")));
    }
}
