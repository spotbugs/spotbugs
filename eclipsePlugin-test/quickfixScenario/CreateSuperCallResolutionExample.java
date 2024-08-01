public class CreateSuperCallResolutionExample extends AbstractCreateSuperCallResolutionExample {
    public void testSomething() {
    }

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
