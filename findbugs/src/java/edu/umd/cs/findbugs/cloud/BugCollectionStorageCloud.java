/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.cloud;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;

/**
 * A basic "cloud" that stores information to the bug collection analysis XML file.
 *
 * @author pwilliam
 */
public class BugCollectionStorageCloud extends AbstractCloud {

    /**
     * Constructor is not protected to allow
     * CloudFactory.createCloudWithoutInitializing() create a new instance of
     * this cloud
     */
    public BugCollectionStorageCloud(CloudPlugin plugin, BugCollection bc, Properties properties) {
        super(plugin, bc, properties);
        setSigninState(SigninState.NO_SIGNIN_REQUIRED);
    }

    @Override
    public boolean initialize() {
        try {
            // we know AbstractCloud.initialize doesn't throw this.
            return super.initialize();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void waitUntilIssueDataDownloaded() {
    }

    public void initiateCommunication() {
    }

    public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }
    
    @Override
    public Mode getMode() {
        return Mode.COMMUNAL;
    }

    public String getUser() {
        return null;
    }

    @SuppressWarnings({"deprecation"})
    @Override
    public UserDesignation getUserDesignation(BugInstance b) {
        BugDesignation bd = b.getUserDesignation();
        if (bd == null)
            return UserDesignation.UNCLASSIFIED;
        return UserDesignation.valueOf(bd.getDesignationKey());
    }

    @SuppressWarnings({"deprecation"})
    @Override
    public String getUserEvaluation(BugInstance b) {
        BugDesignation bd = b.getUserDesignation();
        if (bd == null)
            return "";
        return bd.getAnnotationText();
    }

    @SuppressWarnings({"deprecation"})
    @Override
    public long getUserTimestamp(BugInstance b) {
        BugDesignation bd = b.getUserDesignation();
        if (bd == null)
            return Long.MAX_VALUE;
        return bd.getTimestamp();
    }

    @Override
    public void setMode(Mode m) {
    }

    @Override
    public void bugsPopulated() {
        assert true;

    }

    public void setSaveSignInInformation(boolean save) {
    }

    public boolean isSavingSignInInformationEnabled() {
        return false;
    }

    public void signIn() {
    }

    public void signOut() {
    }

    public boolean availableForInitialization() {
        return true;
    }

    public void storeUserAnnotation(BugInstance bugInstance) {

    }

    public void bugFiled(BugInstance b, Object bugLink) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({"deprecation"})
    public BugDesignation getPrimaryDesignation(BugInstance b) {
        return b.getUserDesignation();
    }

    @Override
    protected Iterable<BugDesignation> getLatestDesignationFromEachUser(BugInstance bd) {
        BugDesignation designation = getPrimaryDesignation(bd);
        if (designation == null)
            return Collections.emptySet();
        return Collections.singleton(designation);
    }

    public Collection<String> getProjects(String className) {
        return Collections.emptyList();
    }

    public boolean isInCloud(BugInstance b) {
        return true;
    }

    public boolean isOnlineCloud() {
        return false;
    }

    public void waitUntilNewIssuesUploaded() {
    }

    @Override
    public void addDateSeen(BugInstance b, long when) {
        if (when > 0)
          b.getXmlProps().setFirstSeen(new Date(when));
    }

}
