package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.WebPageServletTest;

public class AEWebPageServletTest extends WebPageServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineTestHelper());
        super.setUp();
    }
}