package privateFinalLocks;

public class GoodSynchronizationWithInheritance {
    private final Object lock = new Object();

    public void doSomeStuff() {
        synchronized (lock) {
            System.out.println("Do some real stuff");
        }
    }

}

class GoodSynchronizationInheritedFromParent extends GoodSynchronizationWithInheritance {
    private final Object lock = new Object();

    public void doStuff2() {
        synchronized (lock) {
            System.out.println("Do some real stuff");
        }
    }
}
