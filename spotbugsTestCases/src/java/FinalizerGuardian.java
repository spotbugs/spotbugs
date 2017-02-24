class FinalizerGuardian {
    public void free() {
        // free some JNI native resources
    }

    private final Object finalizerGuardian = new Object() {
        @Override
        protected void finalize() {
            free();
        }
    };
}
