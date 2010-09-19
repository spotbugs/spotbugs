package bugPatterns;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class BIT_SIGNED_CHECK {

    static final long HIGH_LONG = 0x8000000000000000L;

    static final int HIGH_INT = 0x80000000;

    static final int LOW = 0x1;

    static final int MEDIUM_INT = 0x10000000;

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighGT(long x) {
        if ((x & HIGH_LONG) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighGE(long x) {
        if ((x & HIGH_LONG) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighLT(long x) {
        if ((x & HIGH_LONG) < 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighLE(long x) {
        if ((x & HIGH_LONG) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowGT(long x) {
        if ((x & LOW) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowGE(long x) {
        if ((x & LOW) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowLT(long x) {
        if ((x & LOW) < 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowLE(long x) {
        if ((x & LOW) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighGT(int x) {
        if ((x & HIGH_INT) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighGE(int x) {
        if ((x & HIGH_INT) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighLT(int x) {
        if ((x & HIGH_INT) < 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugHighLE(int x) {
        if ((x & HIGH_INT) > 0)
            return true;
        return false;
    }

    // / MEDIUM

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugMediumGT(int x) {
        if ((x & MEDIUM_INT) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugMediumGE(int x) {
        if ((x & MEDIUM_INT) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugMediumLT(int x) {
        if ((x & MEDIUM_INT) < 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugMediumLE(int x) {
        if ((x & MEDIUM_INT) > 0)
            return true;
        return false;
    }

    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugNotMediumMask(int x) {
        if ((x & ~MEDIUM_INT) > 0)
            return true;
        return false;
    }

    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugNotMediumMask(long x) {
        if ((x & ~MEDIUM_INT) > 0)
            return true;
        return false;
    }

    // / LOW

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowGT(int x) {
        if ((x & LOW) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowGE(int x) {
        if ((x & LOW) > 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowLT(int x) {
        if ((x & LOW) < 0)
            return true;
        return false;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugLowLE(int x) {
        if ((x & LOW) > 0)
            return true;
        return false;
    }

    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugNotLowMask(int x) {
        if ((x & ~LOW) > 0)
            return true;
        return false;
    }

    @NoWarning("BIT_SIGNED_CHECK_HIGH_BIT")
    public boolean bugNotLowMask(long x) {
        if ((x & ~LOW) > 0)
            return true;
        return false;
    }

}
