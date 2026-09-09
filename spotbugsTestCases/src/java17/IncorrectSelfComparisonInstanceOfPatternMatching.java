/**
 * False positive SA_LOCAL_SELF_COMPARISON reported on number when using instanceof pattern matching in Java 14.
 * 
 * @author Ken Schosinsky
 */
public class IncorrectSelfComparisonInstanceOfPatternMatching {

    public static void instanceofPatternMatching() {
        Number number = 1f;
        if (number instanceof Float f) {
            System.out.println("number is a float: " + f);
        }
    }

}
