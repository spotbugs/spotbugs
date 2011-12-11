package edu.umd.cs.findbugs.updates;

import java.util.Collection;

import edu.umd.cs.findbugs.updates.UpdateChecker.PluginUpdate;

public interface PluginUpdateListener {
    void pluginUpdateCheckComplete(Collection<UpdateChecker.PluginUpdate> updates, boolean force);
}
