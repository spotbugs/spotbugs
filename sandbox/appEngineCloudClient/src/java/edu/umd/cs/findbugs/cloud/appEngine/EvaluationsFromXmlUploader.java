package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.util.Util;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EvaluationsFromXmlUploader {
    private IdentityHashMap<BugInstance, BugDesignation> localAnnotations;
    private final AppEngineCloudClient cloud;
    private AtomicBoolean checkedForUpload = new AtomicBoolean(false);

    public EvaluationsFromXmlUploader(AppEngineCloudClient cloud) {
        this.cloud = cloud;
    }

    public void tryUploadingLocalAnnotations(boolean force) {
        if (!force && !checkedForUpload.compareAndSet(false, true))
            return;
        if (cloud.getGuiCallback().isHeadless())
            return;
        if (cloud.getSigninState() != Cloud.SigninState.SIGNED_IN && !cloud.couldSignIn())
            return;
        localAnnotations = getDesignationsFromXML();

        int num = localAnnotations.size();
        if (num <= 0)
            return;

        cloud.getBackgroundExecutor().execute(new Runnable() {

            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                removeIssuesThatShouldNotBeUploaded();
                System.out.println("Issues to upload: " + localAnnotations.size());
                if (localAnnotations.isEmpty())
                    return;

                cloud.getGuiCallback().invokeInGUIThread(new Runnable() {

                    public void run() {
                        askUserAboutUploadingXMLDesignations();
                    }
                });

            }

        });
    }

    private void removeIssuesThatShouldNotBeUploaded() {
        for (Iterator<Map.Entry<BugInstance, BugDesignation>> i = localAnnotations.entrySet().iterator(); i.hasNext();) {
            Map.Entry<BugInstance, BugDesignation> e = i.next();
            BugInstance b = e.getKey();
            BugDesignation loaded = e.getValue();
            BugDesignation inCloud = cloud.getPrimaryDesignation(b);
            if (!shouldUpload(loaded, inCloud))
                i.remove();
        }
    }

    private void askUserAboutUploadingXMLDesignations() {
        final String userString = getAuthors(localAnnotations);

        String message;
        if (userString.equals(cloud.getUser()))
            message =
                    "The loaded XML file contains " + localAnnotations.size() + " of your evaluations that are more recent than ones stored in the cloud"
                            + "Do you wish to upload these evaluations?";
        else message =
                "The loaded XML file contains " + localAnnotations.size() + " user evaluations of issues by " + userString + "\n"
                        + "Do you wish to upload these evaluations as your evaluations?";
        System.out.println(message);
        int result = cloud.getGuiCallback().showConfirmDialog(message, "Upload evaluations", "Upload", "Skip");
        if (result != IGuiCallback.YES_OPTION)
            return;
        System.out.println(result);
        try {
            cloud.signInIfNecessary("To store your evaluations on the FindBugs Cloud, you must sign in first.");
        } catch (SignInCancelledException e) {
            return;
        }
        if (cloud.getSigninState() != Cloud.SigninState.SIGNED_IN) {
            cloud.getGuiCallback().showMessageDialog("Can't upload evaluations unless you are signed in");
            return;
        }
        cloud.getBackgroundExecutor().execute(new Runnable() {
            @SuppressWarnings({"deprecation"})
            public void run() {
                actuallyUploadXmlEvaluations(localAnnotations);
            }
        });
    }

    /**
     * Given two Bug designations, one from local storage and one in the cloud, should
     * we upload the one from local storage
     *
     * @param loaded
     * @param inCloud
     * @return
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    private boolean shouldUpload(BugDesignation loaded, BugDesignation inCloud) {
        if (inCloud == null)
            return true;
        if (inCloud.getTimestamp() > loaded.getTimestamp())
            return false;
        return !loaded.getDesignationKey().equals(inCloud.getDesignationKey())
                || !Util.nullSafeEquals(loaded.getAnnotationText(), inCloud.getAnnotationText());

    }

    /**
     * @param designationsLoadedFromXML
     * @return
     */
    private String getAuthors(
            final IdentityHashMap<BugInstance, BugDesignation> designationsLoadedFromXML) {
        HashSet<String> users = new HashSet<String>();

        for (BugDesignation bd : designationsLoadedFromXML.values()) {
            String user = bd.getUser();
            if (user == null || user.length() == 0)
                user = "<unknown>";
            users.add(user);
        }
        String userString;
        if (users.size() == 1)
            userString = users.iterator().next();
        else
            userString = users.toString();
        return userString;
    }

    @SuppressWarnings({"deprecation"})
    private void actuallyUploadXmlEvaluations(IdentityHashMap<BugInstance, BugDesignation> designationsLoadedFromXML) {
        int uploaded = 0;
        try {
            cloud.waitUntilNewIssuesUploaded();

            for (Map.Entry<BugInstance, BugDesignation> e : designationsLoadedFromXML.entrySet()) {
                BugInstance b = e.getKey();
                BugDesignation loaded = e.getValue();
                b.setUserDesignation(loaded);
                cloud.storeUserAnnotation(b);
                uploaded++;
            }

            String statusMsg = designationsLoadedFromXML.size() + " issues from XML uploaded to cloud";
            System.out.println(statusMsg);
            cloud.setStatusMsg(statusMsg);
        } catch (Exception e) {
            final String errorMsg = "Unable to upload " + (designationsLoadedFromXML.size() - uploaded)
                    + " issues from XML to cloud due to error\n" + e.getMessage();
            cloud.getGuiCallback().invokeInGUIThread(new Runnable() {
                public void run() {
                    cloud.getGuiCallback().showMessageDialog(
                            errorMsg);

                }
            });


        }
    }

    @SuppressWarnings({"deprecation"})
    private IdentityHashMap<BugInstance, BugDesignation> getDesignationsFromXML() {
        final IdentityHashMap<BugInstance, BugDesignation>
                designationsLoadedFromXML = new IdentityHashMap<BugInstance, BugDesignation>();

        for (BugInstance b : cloud.getBugCollection().getCollection()) {
            BugDesignation bd = b.getUserDesignation();
            if (bd != null)
                designationsLoadedFromXML.put(b, new BugDesignation(bd));
        }
        return designationsLoadedFromXML;
    }
}