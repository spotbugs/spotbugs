package findhidingsubclasstest;

class BadSuperClassWithMain {
    public static int main(String[] args) {
        System.out.println("main (BadSuperClassWithMain)");
        return 1;
    }
}

/**
 * This class is the non-compliant test case for the bug.
 * This test case checks the exceptional case of main method
 * but observe this main method is not the entry point main method due to different signature.
 */
public class BadMainMethodCheck extends BadSuperClassWithMain{
    public static int main(String[] args) {
        System.out.println("main (BadMainMethodCheck)");
        return 2;
    }
}
