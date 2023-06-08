package edu.umd.cs.findbugs.cwe;

import com.google.gson.annotations.SerializedName;

/**
 * Common Weakness Enumeration weakness severity
 *
 * @author Jeremias Eppler
 */
public enum WeaknessSeverity {
    @SuppressWarnings("javadoc")
    @SerializedName("none")
    NONE,

    @SuppressWarnings("javadoc")
    @SerializedName("low")
    LOW,

    @SuppressWarnings("javadoc")
    @SerializedName("medium")
    MEDIUM,

    @SuppressWarnings("javadoc")
    @SerializedName("high")
    HIGH;
}
