package synchronizationOnSharedBuiltinConstant;
 
/**
 * https://wiki.sei.cmu.edu/confluence/display/java/LCK01-J.+Do+not+synchronize+on+objects+that+may+be+reused
 *
 * Misuse of synchronization primitives is a common source of concurrency issues.
 * Synchronizing on objects that may be reused can result in deadlock and nondeterministic
 * behavior. Consequently, programs must never synchronize on objects that may be reused.
 *
 * Noncompliant code samples below
 */
 
public class SynchronizationOnSharedBuiltinConstantBad {
 
    private final Boolean initialized = Boolean.FALSE;
    public void noncompliantBooleanLockObject() {
        synchronized (initialized) {
          System.out.println("noncompliantBooleanLockObject");
        }
    }
 
    private int count = 0;
    private final Integer lock1 = count;
    public void noncompliantBoxedPrimitive() {
        synchronized (lock1) {
            System.out.println("noncompliantBoxedPrimitive");
            count++;
        }
    }
 
    final String lock2 = new String("LOCK").intern();
    public void noncompliantInternedStringObject() {
        synchronized (lock2) {
            System.out.println("noncompliantInternedStringObject");
        }
    }
 
    private final String lock3 = "LOCK";
    public void noncompliantStringLiteral() {
        synchronized (lock3) {
            System.out.println("noncompliantStringLiteral");
        }
    }
}
