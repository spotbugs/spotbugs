package privateFinalLocks;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class ClassExposingItSelf {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
         System.out.println("Do some stuff");
    }

    public synchronized void changeValue() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Change some values");
    }
    
    public static ClassExposingItSelf lookup(String name) {
        return null;
    }

    public static ClassExposingItSelf lookup2(String name) {
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


