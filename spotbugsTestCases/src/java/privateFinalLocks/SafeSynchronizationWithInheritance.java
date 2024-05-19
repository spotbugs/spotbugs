package privateFinalLocks;

public class SafeSynchronizationWithInheritance {
    private final Object lock = new Object();

    public void doSomeStuff() {
        synchronized (lock) {
            System.out.println("Do some real stuff");
        }
    }

}

class SafeSynchronizationInheritedFromParent extends SafeSynchronizationWithInheritance {
    private final Object lock = new Object();

    public void doStuff2() {
        synchronized (lock) {
            System.out.println("Do some real stuff");
        }
    }
}
