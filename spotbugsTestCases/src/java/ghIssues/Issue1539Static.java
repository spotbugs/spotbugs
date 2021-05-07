package ghIssues;

import java.util.Random;

public class Issue1539Static {
    private static final Random RANDOM = new Random();

    public boolean shouldNotReport() {
        return RANDOM.nextBoolean();
    }
}
