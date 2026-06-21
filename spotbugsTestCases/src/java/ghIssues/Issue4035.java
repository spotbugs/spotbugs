package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import java.util.Random;

/** <a href="https://github.com/spotbugs/spotbugs/issues/4035">#4035</a> explicit private ctor. */
public class Issue4035 {
    private Issue4035() {}

    @ExpectWarning("DMI_RANDOM_USED_ONLY_ONCE")
    public static int numberProvider() {
        return new Random().nextInt();
    }

    @ExpectWarning("DMI_RANDOM_USED_ONLY_ONCE")
    public static int boundedProvider(int bound) {
        return new Random().nextInt(bound);
    }
}
