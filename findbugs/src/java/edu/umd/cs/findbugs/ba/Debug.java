package edu.umd.cs.daveho.ba;

public interface Debug {
    /** Set this to true to enable debug print statements. */
    public static final boolean DEBUG = false;

    /**
     * Set this to true to enable data structure integrity checks.
     * These checks are somewhat expensive, but hey, computers are fast.
     */
    public static final boolean VERIFY_INTEGRITY = false /*true*/;
}
