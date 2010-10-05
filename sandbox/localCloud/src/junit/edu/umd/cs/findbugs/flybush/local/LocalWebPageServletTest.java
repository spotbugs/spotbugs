package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.WebPageServletTest;

public class LocalWebPageServletTest extends WebPageServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }
}
