package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.UpdateCheckServletTest;

public class AEUpdateCheckServletTest extends UpdateCheckServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineUpdateTestHelper());
        super.setUp();
    }
}