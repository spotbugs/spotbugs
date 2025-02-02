package lambdas;

import java.util.Map;

/**
 * Return value of INVOKEDYNAMIC used as key to generic container such as a Map.
 * See GitHub issue
 * <a href="https://github.com/spotbugs/spotbugs/issues/500">#500</a>.
 */
public class Issue500 {

    /**
     * Realistic case from production code: using the result of string
     * concatenation (performed within the method) to access a generic
     * collection such as Map.  Won't trigger for code compiled with Java 8
     * because those versions of javac emit a StringBuilder-based sequence.
     * Java 9 javac switched to INVOKEDYNAMIC to choose optimal concatenation
     * strategies at runtime, which revealed the bug.
     */
    Object mapGetFromConcatStr(Map<String, Object> map, String partialKey) {
        String key = "myConstValue" + partialKey; // javac >= 9 uses INDY
        return map.get(key);
    }

    /**
     * To reproduce on Java 8 (where INDY string concatenation doesn't exist),
     * attempt to fetch a value from the provided map by using the 'identity'
     * of a lambda expression privately defined by this method.  It is unlikely
     * that this convoluted construct is of much practical use in real-world
     * code. Choosing to use {@link Runnable} below is completely arbitrary and
     * is not relevant to the bug; it could be any {@code @FunctionalInterface}.
     * 
     * The important part is that the functional interface (FI) is provided by a
     * lambda or a method reference, causing javac to emit INVOKEDYNAMIC, and it
     * is that item that is then used as the key in a generic collection access.
     * Using as a key a traditional reference to some object that implements the
     * interface would not trigger the issue.
     * 
     * Sane code would probably invoke a method on the FI, like
     * {@code Supplier.get()}, and then use that result to do the
     * {@code Map.get()} call.  But that sequence won't trigger the bug,
     * because then the Map generic container access is using the result of
     * an INVOKEINTERFACE instruction. 
     */
    <T> Object mapGetIndyResult(Map<T, Object> map) {
        Runnable funcInterface = System::gc; // gc() is not actually invoked
        return map.get((T) funcInterface);
    }
}
