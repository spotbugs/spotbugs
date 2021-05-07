package ghIssues.issue1539;

import java.util.Random;

public class Issue1539Instance {
    private final Random random;

    public Issue1539Instance(long seed) {
        random = new Random(seed);
    }

    public boolean shouldNotReport() {
        return random.nextBoolean();
    }
}
