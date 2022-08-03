public class ChangePublicToProtectedResolutionExample {
    @Override
    public void finalize() throws Throwable {
        this.toString();
    }
}
