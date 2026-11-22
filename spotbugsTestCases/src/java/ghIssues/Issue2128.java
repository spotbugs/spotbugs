package ghIssues;

import java.util.Random;

public class Issue2128 {
    private static Random random;

    public static int generateOffset(int negativeMin, int positiveMax) {
        if (random == null) {
            random = new Random();
        }

        // [ -negativeMin .. +positiveMax ]
        return random.nextInt(positiveMax + negativeMin + 1) - negativeMin;
    }
}
