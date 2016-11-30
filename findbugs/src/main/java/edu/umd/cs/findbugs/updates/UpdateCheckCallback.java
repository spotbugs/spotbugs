package edu.umd.cs.findbugs.updates;

import java.util.List;

import edu.umd.cs.findbugs.GlobalOptions;

public interface UpdateCheckCallback extends GlobalOptions {
    void pluginUpdateCheckComplete(List<UpdateChecker.PluginUpdate> updates, boolean force);
}
