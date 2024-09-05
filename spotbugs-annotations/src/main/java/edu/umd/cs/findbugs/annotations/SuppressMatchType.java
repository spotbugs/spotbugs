package edu.umd.cs.findbugs.annotations;

public enum SuppressMatchType {

    /**
     * Suppress bugs matching any of:
     * <li/> the given bug type {@link String#startsWith(String)}
     * <li/> the given bug category {@link String#equalsIgnoreCase(String)}
     * <li/> the given bug abbreviation {@link String#equalsIgnoreCase(String)}
     */
    DEFAULT,

    /**
     * Suppress bugs matching the exact (case sensitive) given type.
     */
    EXACT,

    /**
     * Suppress bugs whose type match the given regular expression.
     */
    REGEX;
}
