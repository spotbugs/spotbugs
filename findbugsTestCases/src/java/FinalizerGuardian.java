
class FinalizerGuardian {
    public void free() {
        // free some JNI native resources
    }

    private final Object finalizerGuardian = new Object() {
        protected void finalize() {
            free();
        }
    };
}
