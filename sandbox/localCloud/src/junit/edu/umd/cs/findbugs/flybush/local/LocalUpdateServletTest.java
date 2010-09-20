package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.UpdateServletTest;

public class LocalUpdateServletTest extends UpdateServletTest {
    @Override
    protected void setUp() throws Exception {
        setFlybushServletTestHelper(new LocalFlybushServletTestHelper());
        super.setUp();
    }

    @Override
    public void testUpdateDbJune29CommentStyle() throws Exception {
    }

    @Override
    public void testUpdateDbJune29PrimaryClass() throws Exception {
    }

    @Override
    public void testUpdateDbJune29Package() throws Exception {
    }

    @Override
    public void testUpdateEvaluationEmails() throws Exception {
    }
}