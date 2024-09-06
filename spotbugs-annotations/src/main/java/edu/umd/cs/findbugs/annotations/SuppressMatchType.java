package edu.umd.cs.findbugs.annotations;

public enum SuppressMatchType {

    /**
     * Default bug suppression using a mixed prefixed / case insensitive match depending on the criterion.
     * Suppress bugs matching any of:
     * <ul>
     * <li> the given bug type with: {@link String#startsWith(String)}
     * <li> the given bug category with: {@link String#equalsIgnoreCase(String)}
     * <li> the given bug abbreviation with: {@link String#equalsIgnoreCase(String)}
     * </ul>
     */
    DEFAULT,

    /**
     * Exact (case sensitive match).
     * Suppress bugs matching any of:
     * <ul>
     * <li> the given bug type with: {@link String#equals(Object)}
     * <li> the given bug category with: {@link String#equals(Object)}
     * <li> the given bug abbreviation with: {@link String#equals(Object)}
     * </ul>
     */
    EXACT,

    /**
     * Suppress bugs whose type, category or abbreviation match the given regular expression.
     */
    REGEX;
}
