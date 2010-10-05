package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.ReportServletTest;

public class LocalReportServletTest extends ReportServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }
}
