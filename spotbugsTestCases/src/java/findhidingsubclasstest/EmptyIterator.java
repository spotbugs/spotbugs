package findhidingsubclasstest;

public class EmptyIterator {
    public static int badMethod() {
        return 1;
    }

    private static class OfAtomic extends EmptyIterator {
        public  static int badMethod(){
            return 2;
        }
    }
}