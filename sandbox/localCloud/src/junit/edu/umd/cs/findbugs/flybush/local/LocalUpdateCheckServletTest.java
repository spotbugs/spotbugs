package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.UpdateCheckServletTest;

public class LocalUpdateCheckServletTest extends UpdateCheckServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }

}