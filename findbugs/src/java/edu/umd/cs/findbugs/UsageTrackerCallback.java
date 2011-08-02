package edu.umd.cs.findbugs;

import java.util.List;

public interface UsageTrackerCallback extends GlobalOptions {
    void pluginUpdateCheckComplete(List<UsageTracker.PluginUpdate> updates);
}
