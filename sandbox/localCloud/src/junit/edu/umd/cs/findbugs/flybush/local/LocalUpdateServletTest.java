package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.AuthServletTest;
import edu.umd.cs.findbugs.flybush.UpdateServletTest;

public class LocalUpdateServletTest extends UpdateServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }

}