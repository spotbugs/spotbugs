package sfBugs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class Bug2898106 {

    private static final ConcurrentHashMap<String, Semaphore> locks = new ConcurrentHashMap<String, Semaphore>();

    static Semaphore getLock(String key) {
        Semaphore lock = locks.get(key);
        if (lock == null) {
            Semaphore newLock = new Semaphore(1, true);
            lock = locks.putIfAbsent(key, lock);
            // value, being null, will *always* throw NullPointerException
            if (lock == null)
                lock = newLock;
        }
        return lock;
    }
    // static Semaphore getLock(ConcurrentMap<String, Semaphore> locks2, String
    // key) {
    // Semaphore lock = locks2.get(key);
    // if (lock == null) {
    // Semaphore newLock = new Semaphore(1, true);
    // locks2.get(null);
    // locks2.put(null, null);
    // locks2.remove(null);
    // locks2.containsKey(null);
    // locks2.containsValue(null);
    // locks2.putIfAbsent(null, null);
    // locks2.remove(null, null);
    // locks2.replace(null, null);
    // locks2.replace(null, null, null);
    // lock = locks2.putIfAbsent(key, lock);
    // // value, being null, will *always* throw NullPointerException
    // if (lock == null)
    // lock = newLock;
    // }
    // return lock;
    // }
    // static Semaphore getLock(ConcurrentMap<String, Semaphore> locks2,
    // Semaphore lock , String key) {
    // if (lock == null) {
    // lock = locks2.putIfAbsent(key, lock);
    // }
    // return lock;
    // }

}
