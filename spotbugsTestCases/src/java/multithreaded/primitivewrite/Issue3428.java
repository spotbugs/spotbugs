package multithreaded.primitivewrite;

public class Issue3428 {
    private long directCounter;
    private long indirectCounter;
    private long finallyCounter;

    private void increaseFinallyCounter() {
        try {
            // do something
        } finally {
            finallyCounter++;
        }
    }

    private void increaseIndirectCounter() {
        indirectCounter++;
    }

    public synchronized void directCounterIncreaseSynchronized() {
        directCounter++;
    }

    public synchronized void indirectCounterIncreaseSynchronized() {
        increaseIndirectCounter();
    }

    public synchronized void finallyCounterIncreaseSynchronized() {
        increaseFinallyCounter();
    }

    protected void toStringAppendFields(final StringBuilder builder) {
        builder.append("IndirectCounter =");
        builder.append(indirectCounter);
        builder.append(", DirectCounter =");
        builder.append(directCounter);
        builder.append(", FinallyCounter =");
        builder.append(finallyCounter);
    }
}