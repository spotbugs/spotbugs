package findhidingsubclasstest;

public class OuterInnerClass {
    public static int badMethod() {
        return 1;
    }

    private static class Inner extends OuterInnerClass {
        public  static int badMethod(){
            return 2;
        }
    }
}