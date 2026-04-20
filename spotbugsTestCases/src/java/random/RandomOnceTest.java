package random;

import java.util.Random;

public class RandomOnceTest {

    public int[] fillArrayWithWhile(long seed) {
        final Random random = new Random(seed);
        int[] array = new int[10];
        int i = 0;
        while (i < array.length) {
            array[i] = random.nextInt();
            i++;
        }
        return array;
    }

    public int[] fillArrayWithDowhile(long seed) {
        final Random random = new Random(seed);
        int[] array = new int[10];
        int i = 0;
        do {
            array[i] = random.nextInt();
            i++;
        } while (i < array.length);
        return array;
    }

    public int[] fillArrayWithFor(long seed) {
        final Random random = new Random(seed);
        int[] array = new int[10];
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextInt();
        }
        return array;
    }

    public int useOnceInIf(long seed, boolean useRandom) {
        final Random random = new Random(seed);
        if (useRandom) {
            return random.nextInt();
        } else {
            return 1;
        }
    }

    public int useOnceInSwitch(long seed, int useRandom) {
        final Random random = new Random(seed);
        switch (useRandom) {
            case 1:
            case 2:
                return random.nextInt();
            default:
                return 1;
        }
    }

    public int useOnceInEnhancedSwitch(long seed, int useRandom) {
        final Random random = new Random(seed);
        return switch (useRandom) {
            case 1, 2 -> random.nextInt();
            default -> 1;
        };
    }
}
