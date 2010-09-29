package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.UpdateServletTest;

public class AEUpdateServletTest extends UpdateServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineTestHelper());
        super.setUp();
    }
}