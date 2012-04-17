package edu.umd.cs.findbugs.cloud.appEngine;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.cloud.BugFiler;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;

public class BugFilingHelper {
    private static final Logger LOGGER = Logger.getLogger(BugFilingHelper.class.getName());

    private final WebCloudClient cloud;
    private final BugFiler bugFiler;


    static public BugFiler construct(ComponentPlugin<BugFiler> plugin, Cloud cloud) {

        Class<? extends BugFiler> pluginClass = plugin.getComponentClass();

        try {
            Constructor<? extends BugFiler> constructor = pluginClass.getConstructor(
                    ComponentPlugin.class, Cloud.class);
            return constructor.newInstance(plugin, cloud);
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getCause());

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to construct " + plugin.getId(), e);
        }

    }
    public BugFilingHelper(WebCloudClient webCloudClient,
            ComponentPlugin<BugFiler> componentPlugin) {
        this.cloud = webCloudClient;
        this.bugFiler = construct(componentPlugin, cloud);
    }

    public @CheckForNull String lookupBugStatus(final BugInstance b) {
        if (cloud.getBugLinkStatus(b) == Cloud.BugFilingStatus.FILE_BUG)
            return null;

        if (bugFiler == null)
            return null;

        String status;

        final String bugLink = cloud.getBugLink(b).toExternalForm();
        cloud.getBackgroundExecutor().execute(new Runnable() {
            public void run() {
                String status = null;
                try {
                    status = bugFiler.getBugStatus(bugLink);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while connecting to bug tracker", e);
                }
                if (status == null)
                    status = "<unknown>";
                cloud.updateBugStatusCache(b, status);
            }
        });
        status = "<loading...>";
        cloud.updateBugStatusCache(b, status);
        return status;
    }

    public URL fileBug(BugInstance b) throws SignInCancelledException, Exception {
        if (!bugFilingAvailable())
            throw new IllegalStateException("Bug filing is not available");

        return bugFiler.file(b);
    }

    public boolean bugFilingAvailable() {
        return bugFiler.ready();
    }
}
