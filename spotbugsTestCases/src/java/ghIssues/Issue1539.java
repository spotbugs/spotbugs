package ghIssues;

import java.util.Random;

public class Issue1539 {
    private final Random random = new Random();

    public boolean shouldNotReport() {
        return random.nextBoolean();
    }
}
