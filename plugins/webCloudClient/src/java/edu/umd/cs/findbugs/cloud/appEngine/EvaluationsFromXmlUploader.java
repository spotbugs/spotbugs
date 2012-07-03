package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.util.Multiset;
import edu.umd.cs.findbugs.util.Util;

public class EvaluationsFromXmlUploader {
    private final WebCloudClient cloud;

    private final AtomicBoolean checkedForUpload = new AtomicBoolean(false);

    private final AtomicBoolean triedToUpload = new AtomicBoolean(false);

    
    public EvaluationsFromXmlUploader(WebCloudClient cloud) {
        this.cloud = cloud;
    }

    public void reset() {
        checkedForUpload.set(false);
        triedToUpload.set(false);
    }
    public void tryUploadingLocalAnnotations(boolean force) {
        if (!force && !checkedForUpload.compareAndSet(false, true))
            return;
        if (triedToUpload.get())
            return;
       if (cloud.getGuiCallback().isHeadless())
            return;
        if (cloud.getSigninState().shouldAskToSignIn() && !cloud.couldSignIn())
            return;

        final IdentityHashMap<BugInstance, BugDesignation> localAnnotations = getDesignationsFromXML();
        if (triedToUpload.get())
            return;
       
        int num = localAnnotations.size();
        if (num <= 0)
            return;
        
        cloud.getBackgroundExecutor().execute(new Runnable() {

            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                removeIssuesThatShouldNotBeUploaded(localAnnotations);
                if (localAnnotations.isEmpty())
                    return;
                if (!triedToUpload.compareAndSet(false, true))
                    return;
              
                cloud.getGuiCallback().invokeInGUIThread(new Runnable() {

                    public void run() {
                        askUserAboutUploadingXMLDesignations(localAnnotations);
                    }
                });

            }

        });
    }

    private void removeIssuesThatShouldNotBeUploaded(IdentityHashMap<BugInstance, BugDesignation> localAnnotations ) {
        for (Iterator<Map.Entry<BugInstance, BugDesignation>> i = localAnnotations.entrySet().iterator(); i.hasNext();) {
            Map.Entry<BugInstance, BugDesignation> e = i.next();
            BugInstance b = e.getKey();
            BugDesignation loaded = e.getValue();
            BugDesignation inCloud = cloud.getDesignationByUser(b, loaded.getUser());
            if (!shouldUpload(loaded, inCloud))
                i.remove();
        }
    }

    void askUserAboutUploadingXMLDesignations(final IdentityHashMap<BugInstance, BugDesignation> localAnnotations ) {
        
       Multiset<String> users = getAuthors(localAnnotations);

        String message;
        if (users.numKeys() == 1 && users.uniqueKeys().iterator().next().equals(cloud.getUser()))
            message = "The loaded XML file contains " + localAnnotations.size()
                    + " of your reviews that are more recent than ones stored in the cloud.\n\n"
                    + "Do you wish to upload these reviews?";
        else
            message = "The loaded XML file contains " + authorsToString(users) + "\n\n"
                    + "Do you wish to upload these reviews as your reviews?";

        int result = cloud.getGuiCallback().showConfirmDialog(message, "Upload reviews", "Upload", "Skip");
        if (result != IGuiCallback.YES_OPTION) {
            for(BugInstance b : cloud.getBugCollection()){
                b.setUserAnnotationDirty(false);
            }
            return;
        }
        
        try {
            cloud.signInIfNecessary("To store your reviews on the " + cloud.getCloudName()
                    + ", you must sign in first.");
        } catch (SignInCancelledException e) {
            return;
        }
        if (cloud.getSigninState() != Cloud.SigninState.SIGNED_IN) {
            cloud.getGuiCallback().showMessageDialog("Can't upload reviews unless you are signed in");
            return;
        }
        cloud.getBackgroundExecutor().execute(new Runnable() {
            @SuppressWarnings({ "deprecation" })
            public void run() {
                actuallyUploadXmlEvaluations(localAnnotations);
            }
        });
    }

    /**
     * Given two Bug designations, one from local storage and one in the cloud,
     * should we upload the one from local storage
     * 
     * @param loaded
     * @param inCloud
     * @return
     */
    @SuppressWarnings({ "SimplifiableIfStatement" })
    private boolean shouldUpload(BugDesignation loaded, BugDesignation inCloud) {
        if (inCloud == null)
            return true;
        if (inCloud.getTimestamp() > loaded.getTimestamp())
            return false;
        return !loaded.getDesignationKey().equals(inCloud.getDesignationKey())
                || !Util.nullSafeEquals(loaded.getAnnotationText(), inCloud.getAnnotationText());
    }

    private Multiset<String> getAuthors(final IdentityHashMap<BugInstance, BugDesignation> designationsLoadedFromXML) {
        Multiset<String> users = new Multiset<String>();

        for (BugDesignation bd : designationsLoadedFromXML.values()) {
            String user = bd.getUser();
            if (user == null || user.length() == 0)
                user = "<unknown>";
            users.add(user);
        }
        return users;
    }

    private String authorsToString(Multiset<String> users) {
        StringWriter w = new StringWriter();
        PrintWriter out = new PrintWriter(w);
        int count = 0;
        for (Map.Entry<String, Integer> e : users.entrySet()) {
            if (count > 0) {
                if (count == users.numKeys() - 1)
                    out.print(" and ");
                else
                    out.print(", ");
            }
            out.printf("%d reviews by %s", e.getValue(), e.getKey());
            count++;
        }
        out.close();
        return w.toString();
    }

    @SuppressWarnings({ "deprecation" })
    void actuallyUploadXmlEvaluations(IdentityHashMap<BugInstance, BugDesignation> designationsLoadedFromXML) {
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
           cloud.setStatusMsg(statusMsg);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            
            cloud.getGuiCallback().showMessageDialog(
                    "Unable to upload " + (designationsLoadedFromXML.size() - uploaded)
                            + " issues from XML to cloud due to error\n" + e.getMessage());
        }
    }

    @SuppressWarnings({ "deprecation" })
    private IdentityHashMap<BugInstance, BugDesignation> getDesignationsFromXML() {
        final IdentityHashMap<BugInstance, BugDesignation> designationsLoadedFromXML = new IdentityHashMap<BugInstance, BugDesignation>();

        for (BugInstance b : cloud.getBugCollection().getCollection()) {
            if (!cloud.canStoreUserAnnotation(b))
                continue;
            if (!b.isUserAnnotationDirty())
                continue;
            BugDesignation bd = b.getUserDesignation();
            if (bd != null)
                designationsLoadedFromXML.put(b, new BugDesignation(bd));
        }
        return designationsLoadedFromXML;
    }
}
