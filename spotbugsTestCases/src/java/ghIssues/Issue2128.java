package ghIssues;

import java.security.SecureRandom;
import java.util.Random;

public class Issue2128 {
    private static Random random;

    private Random instanceRandom;

    private static volatile Random doubleCheckedRandom;

    private static Random seededRandom;

    private static SecureRandom secureRandom;

    private static Random earlyRandom;

    private static Random viaLocalRandom;

    public static int generateOffset(int negativeMin, int positiveMax) {
        if (random == null) {
            random = new Random();
        }

        // [ -negativeMin .. +positiveMax ]
        return random.nextInt(positiveMax + negativeMin + 1) - negativeMin;
    }

    public int lazyInstance(int bound) {
        if (instanceRandom == null) {
            instanceRandom = new Random();
        }
        return instanceRandom.nextInt(bound);
    }

    public static int lazyDoubleChecked(int bound) {
        if (doubleCheckedRandom == null) {
            synchronized (Issue2128.class) {
                if (doubleCheckedRandom == null) {
                    doubleCheckedRandom = new Random();
                }
            }
        }
        return doubleCheckedRandom.nextInt(bound);
    }

    public static int lazySeeded(long seed, int bound) {
        if (seededRandom == null) {
            seededRandom = new Random(seed);
        }
        return seededRandom.nextInt(bound);
    }

    public static int lazySecureRandom(int bound) {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        return secureRandom.nextInt(bound);
    }

    public static int lazyEarlyReturn(int bound) {
        if (earlyRandom != null) {
            return earlyRandom.nextInt(bound);
        }
        earlyRandom = new Random();
        return earlyRandom.nextInt(bound);
    }

    public static int lazyViaLocal(int bound) {
        if (viaLocalRandom == null) {
            Random created = new Random();
            viaLocalRandom = created;
        }
        return viaLocalRandom.nextInt(bound);
    }

    public int localUsedOnce(int bound) {
        Random local = new Random();
        return local.nextInt(bound);
    }

    public int localUsedOnceWithSeed(long seed) {
        Random local = new Random(seed);
        return local.nextInt();
    }
}
