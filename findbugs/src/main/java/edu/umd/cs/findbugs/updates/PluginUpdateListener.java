package edu.umd.cs.findbugs.updates;

import java.util.Collection;

public interface PluginUpdateListener {
    void pluginUpdateCheckComplete(Collection<UpdateChecker.PluginUpdate> updates, boolean force);
}
