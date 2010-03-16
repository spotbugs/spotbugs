package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.AuthServletTest;

public class AEAuthServletTest extends AuthServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineTestHelper());
        super.setUp();
    }
}
