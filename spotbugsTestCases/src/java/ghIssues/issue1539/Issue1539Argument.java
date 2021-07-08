package ghIssues.issue1539;

import java.util.Random;

public class Issue1539Argument {
    public static boolean shouldNotReport(final Random random) {
        return random.nextBoolean();
    }
}
