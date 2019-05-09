package edu.umd.cs.findbugs;

import javax.annotation.CheckForNull;

public interface GlobalOptions {
    @CheckForNull
    String getGlobalOption(String key);

    @CheckForNull
    Plugin getGlobalOptionSetter(String key);
}
