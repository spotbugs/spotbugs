package ghIssues.issue1539;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Issue1539ThreadLocal {
    public boolean shouldNotReport() {
        Random random = ThreadLocalRandom.current();
        return random.nextBoolean();
    }
}
