package ghIssues;

import java.util.Random;

public class Issue1518 {
    private final Random RANDOM = new Random();

    public void testDouble() {
        int testDouble = (int)RANDOM.nextDouble();
        System.out.println(testDouble);
    }

    public void testFloat() {
        int testFloat = (int)RANDOM.nextFloat();
        System.out.println(testFloat);
    }

    public void testLong() {
        int testLong = (int)RANDOM.nextLong();
        System.out.println(testLong);
    }
}
