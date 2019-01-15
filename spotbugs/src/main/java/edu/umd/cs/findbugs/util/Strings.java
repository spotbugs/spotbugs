/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * A class for static String utility methods.
 *
 * @author Brian Cole
 */
public class Strings {

    /**
     * This is intended to be semantically equivalent to
     * <code>source.replace(find, repl)</code> but also compatible with JDK 1.4.
     *
     * @param source
     *            The String on which to operate
     * @param find
     *            The literal substring to be replaced
     * @param repl
     *            The literal replacement substring
     * @return The resultant String after substitution
     * @throws NullPointerException
     *             if any of the arguments are null
     * @throws IllegalArgumentException
     *             if <code>find</code> has zero length
     * @see java.lang.String#replace(CharSequence target, CharSequence
     *      replacement)
     */
    @Deprecated
    public static String replace(String source, String find, String repl) {
        return source.replace(find, repl);
    }

    /**
     * This is intended to be equivalent to <code>Arrays.toString(a)</code> but
     * also compatible with JDK 1.4. This concatenates the results of calling
     * String.valueOf() on each element of the array, so this won't work well
     * for multi-dimensional arrays.
     *
     * @see java.lang.String#valueOf(Object)
     * @see java.util.Arrays#toString(Object[])
     * @see java.util.Arrays#deepToString(Object[])
     */
    @Deprecated
    public static String toString(final Object[] a) {
        return Arrays.toString(a);
    }

    /**
     * Trim trailing comma from given string.
     *
     * @param s
     *            a string
     * @return the same string with trailing comma trimmed (if any)
     */
    public static String trimComma(String s) {
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /*
     * XML escape/unescape routines: extend functionality of
     * org.apache.commons.lang.{un,}escapeXml() by supporting low-value
     * character escaping/unescaping
     */

    private static final int xmlAllowedLowCharacterBound = 0x20;

    private static boolean isInvalidXMLCharacter(int c) {
        if ((c < xmlAllowedLowCharacterBound && c >= 0x0 &&
                // low-value characters allowed by XML 1.0 spec
                // '\uFFFE' (&#65534;) cannot be deserialized by SAX reader.
                c != 0x9 && c != 0xA && c != 0xD) || c == 0xFFFE) {
            return true;
        }
        return false;
    }

    private static volatile boolean xmlLowValueEscapeStringsInitialized = false;

    private static final String[] xmlLowValueEscapeStrings = new String[xmlAllowedLowCharacterBound];

    private static final Object escapeInitLockObject = new Object();

    /**
     * Initializes the map of characters to be escaped and their corresponding
     * escape sequences. This method will be invoked automatically the first
     * time a string is escaped/unescaped.
     *
     * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">Extensible Markup
     *      Language (XML) 1.0 (Fifth Edition)</a>
     */
    public static void initializeEscapeMap() {
        if (xmlLowValueEscapeStringsInitialized == true) {
            return;
        }

        /*
         * synchronize the lazy initialization so things don't break if FindBugs
         * ever becomes multi-threaded (and also so FindBugs doesn't throw a
         * warning about the thread safety of this operation)
         */
        synchronized (escapeInitLockObject) {
            if (xmlLowValueEscapeStringsInitialized == true) {
                return;
            }

            for (int i = 0; i < xmlAllowedLowCharacterBound; i++) {
                if (isInvalidXMLCharacter(i)) {
                    String escapedString = String.format("\\u%04x", i);
                    xmlLowValueEscapeStrings[i] = escapedString;
                } else {
                    xmlLowValueEscapeStrings[i] = null;
                }
            }
            xmlLowValueEscapeStringsInitialized = true;
        }
    }

    /**
     * Escape XML entities and illegal characters in the given string. This
     * enhances the functionality of
     * org.apache.commons.lang.StringEscapeUtils.escapeXml by escaping
     * low-valued unprintable characters, which are not permitted by the W3C XML
     * 1.0 specification.
     *
     * @param s
     *            a string
     * @return the same string with characters not permitted by the XML
     *         specification escaped
     * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">Extensible Markup
     *      Language (XML) 1.0 (Fifth Edition)</a>
     * @see <a
     *      href="http://commons.apache.org/lang/api/org/apache/commons/lang/StringEscapeUtils.html#escapeXml(java.lang.String)">org.apache.commons.lang.StringEscapeUtils
     *      javadoc</a>
     */
    public static String escapeXml(String s) {
        initializeEscapeMap();

        if (s == null || s.length() == 0) {
            return s;
        }

        char[] sChars = s.toCharArray();
        StringBuilder sb = new StringBuilder();
        int lastReplacement = 0;
        for (int i = 0; i < sChars.length; i++) {
            if (isInvalidXMLCharacter(sChars[i])) {
                // append intermediate string to string builder
                sb.append(sChars, lastReplacement, i - lastReplacement);
                // substitute control character with escape sequence
                sb.append(sChars[i] == 0xFFFE ? "\\ufffe" : xmlLowValueEscapeStrings[sChars[i]]);
                // advance last pointer past this character
                lastReplacement = i + 1;
            }
        }
        if (lastReplacement < sChars.length) {
            sb.append(sChars, lastReplacement, sChars.length - lastReplacement);
        }

        return StringEscapeUtils.escapeXml(sb.toString());
    }

    private static final String unicodeUnescapeMatchExpression = "(\\\\*)(\\\\u)(\\p{XDigit}{4})";

    private static Pattern unescapePattern = null;

    private static volatile boolean paternIsInitialized = false;

    private static final Object unescapeInitLockObject = new Object();

    /**
     * Initialize regular expressions used in unescaping. This method will be
     * invoked automatically the first time a string is unescaped.
     */
    public static boolean initializeUnescapePattern() {
        if (paternIsInitialized == true) {
            return true;
        }

        synchronized (unescapeInitLockObject) {
            if (paternIsInitialized == true) {
                return true;
            }

            try {
                unescapePattern = Pattern.compile(unicodeUnescapeMatchExpression);
            } catch (PatternSyntaxException pse) {
                /*
                 * the pattern is compiled from a final string, so this
                 * exception should never be thrown
                 */
                System.err.println("Imposible error:  " + "static final regular expression pattern "
                        + "failed to compile.  Exception:  " + pse.toString());
                return false;
            }
            paternIsInitialized = true;
        }
        return true;
    }

    /**
     * Unescape XML entities and illegal characters in the given string. This
     * enhances the functionality of
     * org.apache.commons.lang.StringEscapeUtils.unescapeXml by unescaping
     * low-valued unprintable characters, which are not permitted by the W3C XML
     * 1.0 specification.
     *
     * @param s
     *            a string
     * @return the same string with XML entities/escape sequences unescaped
     * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">Extensible Markup
     *      Language (XML) 1.0 (Fifth Edition)</a>
     * @see <a
     *      href="http://commons.apache.org/lang/api/org/apache/commons/lang/StringEscapeUtils.html#unescapeXml(java.lang.String)">org.apache.commons.lang.StringEscapeUtils
     *      javadoc</a>
     */
    public static String unescapeXml(String s) {

        initializeEscapeMap();

        /*
         * we can't escape the string if the pattern doesn't compile! (but that
         * should never happen since the pattern is static)
         */
        if (!initializeUnescapePattern()) {
            return s;
        }

        if (s == null || s.length() == 0) {
            return s;
        }

        /*
         * skip this expensive check entirely if there are no substrings
         * resembling Unicode escape sequences in the string to be unescaped
         */
        if (s.contains("\\u")) {
            StringBuffer sUnescaped = new StringBuffer();
            Matcher m = unescapePattern.matcher(s);
            while (m.find() == true) {
                String slashes = m.group(1);
                String digits = m.group(3);
                int escapeCode;
                try {
                    escapeCode = Integer.parseInt(digits, 16);
                } catch (NumberFormatException nfe) {
                    /*
                     * the static regular expression string should guarantee
                     * that this exception is never thrown
                     */
                    System.err.println("Impossible error: escape sequence '" + digits + "' is not a valid hex number!  "
                            + "Exception: " + nfe.toString());
                    return s;
                }
                if (slashes != null && slashes.length() % 2 == 0 && isInvalidXMLCharacter(escapeCode)) {
                    Character escapedSequence = Character.valueOf((char) escapeCode);
                    /*
                     * slashes are apparently escaped when the string buffer is
                     * converted to a string, so double them to make sure the
                     * correct number appear in the final representation
                     */
                    m.appendReplacement(sUnescaped, slashes + slashes + escapedSequence.toString());
                }
            }
            m.appendTail(sUnescaped);
            s = sUnescaped.toString();
        }
        return StringEscapeUtils.unescapeXml(s);
    }

    public static String escapeLFCRBackSlash(String s) {
        s = s.replace("\\", "\\\\");
        s = s.replace("\r", "\\r");
        s = s.replace("\n", "\\n");
        return s;
    }

}
