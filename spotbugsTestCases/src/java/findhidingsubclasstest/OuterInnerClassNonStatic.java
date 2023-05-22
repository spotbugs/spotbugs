package findhidingsubclasstest;

public class OuterInnerClassNonStatic {
    private static final OuterInnerClassNonStatic theInstance = new OuterInnerClassNonStatic();
    public void method1(){}
    public static class InnerNonStatic extends OuterInnerClassNonStatic {
        public final static InnerNonStatic goodField = new InnerNonStatic();

        public void method1(){}
    }
}