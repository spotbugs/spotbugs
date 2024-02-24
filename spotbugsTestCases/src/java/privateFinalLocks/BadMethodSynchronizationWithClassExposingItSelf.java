package privateFinalLocks;

class BadMethodSynchronizationWithClassExposingItSelf {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public synchronized void doStuff2() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static BadMethodSynchronizationWithClassExposingItSelf lookup(String name) {
        return null;
    }

    public static BadMethodSynchronizationWithClassExposingItSelf lookup2(String name) {
        return null;
    }
}
