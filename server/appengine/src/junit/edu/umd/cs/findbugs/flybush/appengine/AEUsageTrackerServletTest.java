package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.UsageTrackerServletTest;

public class AEUsageTrackerServletTest extends UsageTrackerServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineTestHelper());
        super.setUp();
    }
}