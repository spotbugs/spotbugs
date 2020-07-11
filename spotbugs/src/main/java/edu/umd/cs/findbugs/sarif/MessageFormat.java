package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Class to parse longDescription to generate formatted test for SARIF.
 * All the logic is copied from {@code FindBugsMessageFormat}.
 * @see edu.umd.cs.findbugs.FindBugsMessageFormat
 */
class MessageFormat {
    private final String pattern;

    MessageFormat(@NonNull String pattern) {
        this.pattern = Objects.requireNonNull(pattern);
    }

    String format(@NonNull BiFunction<Integer, String, String> handler) {
        Objects.requireNonNull(handler);
        String pat = pattern;
        StringBuilder result = new StringBuilder();

        while (pat.length() > 0) {
            int subst = pat.indexOf('{');
            if (subst < 0) {
                result.append(pat);
                break;
            }

            result.append(pat.substring(0, subst));
            pat = pat.substring(subst + 1);

            int end = pat.indexOf('}');
            if (end < 0) {
                throw new IllegalStateException("unmatched { in " + pat);
            }

            String substPat = pat.substring(0, end);

            int dot = substPat.indexOf('.');
            String key = "";
            if (dot >= 0) {
                key = substPat.substring(dot + 1);
                substPat = substPat.substring(0, dot);
            }

            int fieldNum;
            try {
                fieldNum = Integer.parseInt(substPat);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad integer value " + substPat + " in " + pattern);
            }

            if (fieldNum < 0) {
                throw new IllegalArgumentException("The given fieldNum was negative: " + fieldNum);
            }
            try {
                result.append(handler.apply(fieldNum, key));
            } catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException("Problem processing " + pattern + " format " + substPat, iae);
            }
            pat = pat.substring(end + 1);
        }

        return result.toString();
    }
}
