package de.tobject.findbugs.util;

import java.util.Map;

public class SafeHtml {
    private static final Map<String, String> CHARS_TO_ESCAPE = Map.of(
            ">", "&gt;",
            "<", "&lt;",
            "&", "&amp;");

    public static String escape(String s) {
        for (Map.Entry<String, String> pair : CHARS_TO_ESCAPE.entrySet()) {
            s = s.replace(pair.getKey(), pair.getValue());
        }
        return s;
    }
}
