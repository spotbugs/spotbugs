package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.QueryServletTest;

public class AEQueryServletTest extends QueryServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineTestHelper());
        super.setUp();
    }
}