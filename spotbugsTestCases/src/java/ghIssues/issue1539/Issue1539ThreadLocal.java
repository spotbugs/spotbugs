package ghIssues.issue1539;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Issue1539ThreadLocal {

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    public boolean shouldNotReport() {
        return useRandom(random());
    }

    public boolean useRandom(final Random random) {
        return random.nextBoolean();
    }
}
