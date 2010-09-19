package sfBugs;

/**
 * Submitted By: Nils Kilden-Pedersen Summary:
 * 
 * I have a method that returns an static final empty array, yet I get the
 * following error:
 * 
 * M V EI: MyClass.myMethod() may expose internal representation by returning
 * MyClass.EMPTY_ARRAY
 * 
 * EMPTY_ARRAY is defined like this:
 * 
 * private static final Object[] EMPTY_ARRAY = new Object[0];
 * 
 * This is using Eclipse 3.3RC3 with FindBugs 1.2.1.20070531
 * 
 */
public class Bug1739878 {
    private static final Object[] EMPTY_ARRAY = new Object[0];

    // public static void main(String args[]) {
    // Bug1739878 b = new Bug1739878();
    // //b.test();
    // for(int i = 0; i < EMPTY_ARRAY.length; i++) {
    // System.out.println(EMPTY_ARRAY[i] + " ");
    // }
    // }

    // // grep -A 1 | grep Bug1739878
    public Object[] myMethod() {
        return EMPTY_ARRAY;
    }

    // public void test() {
    // Object[] os = myMethod();
    // os = new Integer[] {1, 2};
    // }
}
