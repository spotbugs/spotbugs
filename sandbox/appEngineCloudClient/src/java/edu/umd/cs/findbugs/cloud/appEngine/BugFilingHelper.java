package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.util.ServiceException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;

public class BugFilingHelper {
    private static final Logger LOGGER = Logger.getLogger(BugFilingHelper.class.getName());

    private final AppEngineCloudClient appEngineCloudClient;
    private PropertyBundle properties;
    private GoogleCodeBugFiler googleCodeBugFiler;
    private JiraBugFiler jiraBugFiler;

    public BugFilingHelper(AppEngineCloudClient appEngineCloudClient, PropertyBundle properties) {
        this.appEngineCloudClient = appEngineCloudClient;
        this.properties = properties;
        googleCodeBugFiler = new GoogleCodeBugFiler(appEngineCloudClient);
        jiraBugFiler = new JiraBugFiler(appEngineCloudClient);
    }

    public String lookupBugStatus(final BugInstance b) {

        if (appEngineCloudClient.getBugLinkStatus(b) == Cloud.BugFilingStatus.FILE_BUG)
            return null;

        String status;
        final BugFiler bugFiler = getBugFiler(b);

        final String bugLink = appEngineCloudClient.getBugLink(b).toExternalForm();
        appEngineCloudClient.getBackgroundExecutor().execute(new Runnable() {
            public void run() {
                String status = null;
                try {
                    status = bugFiler.getBugStatus(bugLink);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while connecting to bug tracker", e);
                }
                if (status == null)
                    status = "<unknown>";
                appEngineCloudClient.updateBugStatusCache(b, status);
            }
        });
        status = "<loading...>";
        appEngineCloudClient.updateBugStatusCache(b, status);
        return status;
    }

	/**
	 * @param b
	 * @return
	 */
	private @CheckForNull BugFiler getBugFiler(final BugInstance b) {
		String hash = b.getInstanceHash();
        String linkType = appEngineCloudClient.getBugLinkType(appEngineCloudClient.getBugByHash(hash));
        if (linkType == null)
            linkType = "GOOGLE_CODE";

        BugFiler bugFiler;
        if (linkType.equals("GOOGLE_CODE"))
            return  googleCodeBugFiler;
        else if (linkType.equals("JIRA"))
        		return jiraBugFiler;
        else
            return null;
	}

    public URL fileBug(BugInstance b, String bugLinkType)
            throws javax.xml.rpc.ServiceException, IOException, SignInCancelledException, OAuthException,
                   InterruptedException, ServiceException {
        String trackerUrl = properties.getProperty("cloud.bugTrackerUrl");
        if (trackerUrl != null && trackerUrl.trim().length() == 0)
            trackerUrl = null;

        if ("GOOGLE_CODE".equals(bugLinkType)) {
            return googleCodeBugFiler.file(b, trackerUrl);

        } else if ("JIRA".equals(bugLinkType)) {
            return jiraBugFiler.file(b, trackerUrl);

        } else {
            throw new IllegalArgumentException("Unknown issue tracker " + bugLinkType);
        }
    }

    // ============================== end of public methods ==============================

}