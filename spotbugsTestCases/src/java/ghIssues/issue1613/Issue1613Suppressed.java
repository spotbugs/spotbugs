package ghIssues.issue1613;

import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Issue1613Suppressed {
    @SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "we need just ONE random number ONCE")
    private float hue = new Random().nextFloat();
}
