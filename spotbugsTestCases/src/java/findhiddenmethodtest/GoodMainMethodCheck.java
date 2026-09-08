package findhiddenmethodtest;

class GoodSuperClassWithMain {
    public static void main(String[] args) {
        System.out.println("main (GoodSuperClassWithMain)");
    }
}

/**
 * This class is the compliant test case for the bug.
 * This test case checks the exceptional case of main method.
 */
public class GoodMainMethodCheck extends GoodSuperClassWithMain
{
    public static void main(String[] args) {
        System.out.println("main (GoodMainMethodCheck)");
    }
}
