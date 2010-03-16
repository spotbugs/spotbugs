package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.AuthServletTest;

public class LocalAuthServletTest extends AuthServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }

}