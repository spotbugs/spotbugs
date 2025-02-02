public class ChangePublicToProtectedResolutionExample {
    @Override
    protected void finalize() throws Throwable {
        this.toString();
    }
}
