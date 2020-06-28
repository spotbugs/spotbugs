package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Priorities;
import org.json.JSONString;

/**
 * An enum representing {@code level} property.
 *
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012604">3.27.10 level property</a>
 */
enum Level implements JSONString {
    /**
     * The rule specified by ruleId was evaluated and a problem was found.
     */
    WARNING,
    /**
     * The rule specified by ruleId was evaluated and a serious problem was found.
     */
    ERROR,
    /**
     * The rule specified by ruleId was evaluated and a minor problem or an opportunity to improve the code was found.
     */
    NOTE,
    /**
     * The concept of “severity” does not apply to this result because the kind property (§3.27.9) has a value other than "fail".
     */
    NONE;

    @Override
    public String toJSONString() {
        return String.format("\"%s\"", name().toLowerCase());
    }

    static Level fromPriority(int priority) {
        switch (priority) {
            case Priorities.HIGH_PRIORITY:
                return ERROR;
            case Priorities.NORMAL_PRIORITY:
            case Priorities.LOW_PRIORITY:
                return WARNING;
            case Priorities.EXP_PRIORITY:
            case Priorities.IGNORE_PRIORITY:
                return NOTE;
            default:
                throw new IllegalArgumentException(String.format("Unknown property %d is given", priority));
        }
    }
}
