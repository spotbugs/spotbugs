package findhiddenmethodtest;

/**
 * This is a non-compliant test case as the methods are declared static and non-private,
 * There is inheritance so method hiding as well,
 * but they are in an inner and outer class relation.
 */
public class BadOuterInnerClass {
    public static int badMethod() {
        return 1;
    }

    private static class BadInner extends BadOuterInnerClass {
        public  static int badMethod(){
            return 2;
        }
    }
}
