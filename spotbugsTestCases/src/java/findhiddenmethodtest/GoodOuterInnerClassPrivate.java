package findhiddenmethodtest;

/**
 * This is a compliant test case as the methods are declared static but private,
 * There is no inheritance as the methods are private,
 * but they are in an inner and outer class relation.
 */
public class GoodOuterInnerClassPrivate {
    private static void method1(){
        System.out.println("method1 (GoodOuterInnerClassPrivate)");
    }
    public static class GoodInnerClassPrivate extends GoodOuterInnerClassPrivate {
        private static void method1(){
            System.out.println("method1 (GoodInnerClassPrivate)");
        }
    }
}
