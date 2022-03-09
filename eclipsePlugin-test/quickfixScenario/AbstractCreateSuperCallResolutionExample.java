import junit.framework.TestCase;

public abstract class AbstractCreateSuperCallResolutionExample extends TestCase {
    @Override
    protected void finalize() throws Throwable {
        this.toString();
    }

    @Override
    protected void setUp() throws Exception {
        this.toString();
    }

    @Override
    protected void tearDown() throws Exception {
        this.toString();
    }
}
