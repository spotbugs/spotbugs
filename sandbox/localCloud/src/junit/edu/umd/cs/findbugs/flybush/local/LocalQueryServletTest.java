package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.QueryServletTest;
import edu.umd.cs.findbugs.flybush.UpdateServletTest;

public class LocalQueryServletTest extends QueryServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }

}