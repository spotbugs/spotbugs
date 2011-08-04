package edu.umd.cs.findbugs;

import java.util.List;

public interface UpdateCheckCallback extends GlobalOptions {
    void pluginUpdateCheckComplete(List<UpdateChecker.PluginUpdate> updates);
}
