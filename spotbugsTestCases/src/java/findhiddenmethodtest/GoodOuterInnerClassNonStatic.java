package findhiddenmethodtest;

/**
 * This is a compliant test case as the methods are declared non-static and non-private,
 * There is an inheritance thereof method overriding,
 * but they are in an inner and outer class relation.
 */
public class GoodOuterInnerClassNonStatic {
    public void method1(){
        System.out.println("method1 (GoodOuterInnerClassPrivate)");
    }
    public static class GoodInnerNonStaticGood extends GoodOuterInnerClassNonStatic {
        public void method1(){
            System.out.println("method1 (GoodOuterInnerClassPrivate)");
        }
    }
}
