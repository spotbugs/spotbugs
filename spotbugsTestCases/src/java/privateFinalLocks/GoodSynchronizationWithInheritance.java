package privateFinalLocks;

public class GoodSynchronizationWithInheritance {
    private final Object lock = new Object();

    public void doSomeStuff() {
        synchronized (lock) {
            stuff();
        }
    }

    protected void stuff() {
        System.out.println("Do some real stuff here..");
    }
}

class GoodSynchronizationInheritedFromParent extends GoodSynchronizationWithInheritance {
    private final Object lock = new Object();

    public void doSomeStuff2() {
        synchronized (lock) {
            stuff();
        }
    }
}
