package edu.umd.cs.findbugs;

import com.github.spotbugs.jsr305.annotation.CheckForNull;

public interface GlobalOptions {
    @CheckForNull
    String getGlobalOption(String key);

    @CheckForNull
    Plugin getGlobalOptionSetter(String key);
}
