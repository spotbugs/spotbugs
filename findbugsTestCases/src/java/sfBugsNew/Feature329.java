package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature329 {
    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int checkBounds(int rawInput) {
        return Math.min(0, Math.max(100, rawInput));
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int checkBounds2(int rawInput) {
        return Math.min(0, Math.max(rawInput, 100));
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int checkBounds3(int rawInput) {
        return Math.min(Math.max(rawInput, 100), 0);
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int checkBounds4(int rawInput) {
        return Math.min(Math.max(100, rawInput), 0);
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int checkBounds5(int rawInput) {
        return Math.max(Math.min(0, rawInput), 100);
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int checkBounds6(int rawInput) {
        rawInput = Math.min(0, rawInput);
        rawInput = Math.max(100, rawInput);
        return rawInput;
    }
    
    @ExpectWarning("DM_INVALID_MIN_MAX")
    public int getScore(int totalCount, int failCount, double scaleFactor) {
        // Based on https://github.com/marksinclair/junit-plugin/commit/c0dc11e08923edd23cee90962da638e4a7eb47d5
        int score = (totalCount == 0) ? 100 : (int) (100.0 * Math.max(1.0,
                Math.min(0.0, 1.0 - (scaleFactor * failCount) / totalCount)));
        return score;
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public long checkBounds(long rawInput) {
        return Math.min(0, Math.max(100, rawInput));
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public float checkBounds(float rawInput) {
        return Math.min(0, Math.max(100, rawInput));
    }

    @ExpectWarning("DM_INVALID_MIN_MAX")
    public double checkBounds(double rawInput) {
        return Math.min(0, Math.max(100, rawInput));
    }

    @NoWarning("DM_INVALID_MIN_MAX")
    public int checkBoundsCorrect(int rawInput) {
        return Math.min(100, Math.max(0, rawInput));
    }

    @NoWarning("DM_INVALID_MIN_MAX")
    public int checkBoundsCorrect2(int rawInput) {
        return Math.max(0, Math.min(100, rawInput));
    }

    @NoWarning("DM_INVALID_MIN_MAX")
    public int checkBoundsCorrect3(int rawInput) {
        rawInput = Math.min(100, rawInput);
        rawInput = Math.max(0, rawInput);
        return rawInput;
    }

    @NoWarning("DM_INVALID_MIN_MAX")
    public long checkBoundsCorrect(long rawInput) {
        return Math.min(100, Math.max(0, rawInput));
    }

    @NoWarning("DM_INVALID_MIN_MAX")
    public float checkBoundsCorrect(float rawInput) {
        return Math.min(100, Math.max(0, rawInput));
    }

    @NoWarning("DM_INVALID_MIN_MAX")
    public double checkBoundsCorrect(double rawInput) {
        return Math.min(100, Math.max(0, rawInput));
    }
}
