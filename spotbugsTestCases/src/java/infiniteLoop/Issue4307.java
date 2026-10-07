package infiniteLoop;

public class Issue4307 {

    public int testTerminatingFloatLoop() {
        int start = 1_234_567_890;
        for (float value = start; value < start + 50; value++) {
        }
        return 0;
    }

    public int testInfiniteFloatLoop() {
        float value = 0;
        while (value < 10) {
        }
        return 0;
    }

    public int testTerminatingDoubleLoop() {
        double value = 0;
        for (; value < 10; value++) {
        }
        return 0;
    }

    public int testInfiniteDoubleLoop() {
        double value = 0;
        while (value < 10) {
        }
        return 0;
    }

    public int testComparisonResultDoesNotLeak() {
        float value = 0;
        float limit = 10;

        if (value < limit) {
            value++;
        }

        if (value == 100) {
            value++;
        }

        return 0;
    }
}
