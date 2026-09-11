package ghIssues;

import java.util.Random;

public class Issue1806 {

    private final Random random;

    public Issue1806(long seed) {
        this.random = new Random(seed);
        var maxWidth = 10;
        for (int i = 0; i < 10; i++) {
            int max = random.nextInt(maxWidth) + 1; // reported here
            for (int j = 0; j < max; j++) {
                double solution = random.nextDouble() * 1600 - 800; // reported here
                System.out.println(solution + " / " + random.nextDouble());
            }
        }
    }
}
