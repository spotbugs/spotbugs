package de.tobject.findbugs.util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SafeHtml {
    private static final Map<String, String> CHARS_TO_ESCAPE = Stream.of(new Object[][] {
        { ">", "&gt;" },
        { "<", "&lt;" },
        { "&", "&amp;" }
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));

    public static String escape(String s) {
        for (Map.Entry<String, String> pair : CHARS_TO_ESCAPE.entrySet()) {
            s = s.replace(pair.getKey(), pair.getValue());
        }
        return s;
    }
}
