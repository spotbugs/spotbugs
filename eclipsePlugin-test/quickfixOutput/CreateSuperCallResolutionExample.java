public class CreateSuperCallResolutionExample extends AbstractCreateSuperCallResolutionExample {
    public void testSomething() {
    }

    @Override
    protected void finalize() throws Throwable {
        this.toString();
        super.finalize();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.toString();
    }

    @Override
    protected void tearDown() throws Exception {
        this.toString();
        super.tearDown();
    }
}
