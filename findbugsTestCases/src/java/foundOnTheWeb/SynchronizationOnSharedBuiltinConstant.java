package foundOnTheWeb;

/**
 * http://www.javalobby.org/java/forums/t96352.html
 * 
 * Code that is using String literals to synchronize on is dangerous. See the
 * example below:
 * 
 * <pre>
 * class Foo {
 *     static private final String LOCK = &quot;LOCK&quot;;
 * 
 *     void someMethod() {
 *     synchronized(LOCK) {
 *     ...
 *     }
 *   }
 * }
 * </pre>
 * 
 * Why is this dangerous? I have a nice private String which is mine and mine
 * alone? Or is it? No, it isn't!
 * 
 * Recall section 3.10.5 of the Java Language Spec 2.0: It says among other
 * things:
 * "Literal strings within different classes in different packages likewise represent references to the same String object."
 * 
 * For the code above this means that any number of foreign classes can contain
 * the same literal which translates to the same object, hence creating
 * potential dead-lock situations! This is also true for Strings for which you
 * call the intern() method!
 * 
 * And this is not only a nice theory, something like this really happened
 * between our code and code in the Jetty library. Both sections used the same
 * string literal above to synchronize critical code sections. The two code
 * segments created a dead-lock with very puzzling stack traces (The Jetty-Bug
 * has been reported already, btw, Jetty-352)
 * 
 */
public class SynchronizationOnSharedBuiltinConstant {
    static private final String LOCK = "LOCK";

    void someMethod() {
        synchronized (LOCK) {

        }
    }

}
