package privateFinalLocks;

import java.util.*;

public class BadMethodSynchronizationLock {
    public static void unTrustedCode() throws InterruptedException {
        // Untrusted code
        String name = "untrusted";
        ClassExposingItSelf someObject = ClassExposingItSelf.lookup(name);
        synchronized (someObject) {
            while (true) {
                // Indefinitely lock someObject
                // In some time somObject.changeValue will be called that tries to lock on 'itself'(intrinsic lock)
                // but because of the above synchronized block it won't acquire the lock
                // resulting in a dead lock
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }

    public static void unTrustedCode2() throws InterruptedException {
        // Similar case to the above one, but using a Collection
        Collection<ClassExposingACollectionOfItself> collectionOfItselves = ClassExposingACollectionOfItself.getCollection();

        synchronized (collectionOfItselves.iterator().next()) {
            while (true) {
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }

    public static void unTrustedCode3() throws InterruptedException {
        // Similar case to the above one, but using a Map
        Map<Object, ClassExposingAMapOfItself> aMapOfItself = ClassExposingAMapOfItself.getMap();

        synchronized (aMapOfItself.values().iterator().next()) {
            while (true) {
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }
}


class ClassExposingItSelf {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static synchronized void changeValue() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Change some values");
    }

    public static ClassExposingItSelf lookup(String name) { // exposing the lock object, bug should be detected here
        return null;
    }
}

class ClassExposingACollectionOfItself {
    public synchronized void doStuff() {  // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Collection<ClassExposingACollectionOfItself> getCollection() {
        return Collections.singletonList(new ClassExposingACollectionOfItself());
    }
}

class ClassExposingAMapOfItself {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Map<Object, ClassExposingAMapOfItself> getMap() {
        return Collections.singletonMap(new Object(), new ClassExposingAMapOfItself());
    }
}


