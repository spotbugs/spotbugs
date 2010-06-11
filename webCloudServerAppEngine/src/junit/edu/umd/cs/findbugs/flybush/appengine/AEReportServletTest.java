package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.ReportServletTest;

public class AEReportServletTest extends ReportServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new AppEngineTestHelper());
        super.setUp();
    }
}