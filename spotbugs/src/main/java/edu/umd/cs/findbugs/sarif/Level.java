package edu.umd.cs.findbugs.sarif;

import com.google.gson.annotations.SerializedName;

import edu.umd.cs.findbugs.BugRankCategory;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.cwe.WeaknessSeverity;

/**
 * An enum representing {@code level} property.
 *
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012604">3.27.10 level property</a>
 */
enum Level {
    /**
     * The rule specified by ruleId was evaluated and a problem was found.
     */
    @SerializedName("warning")
    WARNING,
    /**
     * The rule specified by ruleId was evaluated and a serious problem was found.
     */
    @SerializedName("error")
    ERROR,
    /**
     * The rule specified by ruleId was evaluated and a minor problem or an opportunity to improve the code was found.
     */
    @SerializedName("note")
    NOTE,
    /**
     * The concept of “severity” does not apply to this result because the kind property (§3.27.9) has a value other than "fail".
     */
    @SerializedName("none")
    NONE;

    public String toJsonString() {
        return name().toLowerCase();
    }

    @NonNull
    static Level fromBugRank(int bugRank) {
        BugRankCategory category = BugRankCategory.getRank(bugRank);
        switch (category) {
        case SCARIEST:
        case SCARY:
            return ERROR;
        case TROUBLING:
            return WARNING;
        case OF_CONCERN:
            return NOTE;
        default:
            throw new IllegalArgumentException("Illegal bugRank given: " + bugRank);
        }
    }

    @NonNull
    static Level fromWeaknessSeverity(WeaknessSeverity severity) {
        switch (severity) {
        case HIGH:
            return ERROR;
        case MEDIUM:
            return WARNING;
        case LOW:
            return NOTE;
        default:
            return NONE;
        }
    }
}
