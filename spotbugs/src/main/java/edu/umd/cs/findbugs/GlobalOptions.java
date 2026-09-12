package edu.umd.cs.findbugs;

import org.jspecify.annotations.Nullable;

public interface GlobalOptions {
    @Nullable
    String getGlobalOption(String key);

    @Nullable
    Plugin getGlobalOptionSetter(String key);
}
