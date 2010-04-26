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
import java.util.Properties;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.username.NoNameLookup;


/**
 * @author pwilliam
 */
public class BugCollectionStorageCloud extends AbstractCloud {

    private static CloudPlugin getFallbackPlugin() {
        return new CloudPlugin("fallback local cloud", BugCollectionStorageCloud.class.getClassLoader(),
                               BugCollectionStorageCloud.class, NoNameLookup.class, new PropertyBundle(),
                               "no description", "no details");
    }

    BugCollectionStorageCloud(CloudPlugin plugin, BugCollection bc, Properties properties) {
        super(plugin, bc, properties);
        setSigninState(SigninState.NO_SIGNIN_REQUIRED);
    }

    BugCollectionStorageCloud(BugCollection bc) {
        this(getFallbackPlugin(), bc, new Properties());
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

    @Override
    public Mode getMode() {
        return Mode.COMMUNAL;
    }

	public String getUser() {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public UserDesignation getUserDesignation(BugInstance b) {
	    BugDesignation bd = b.getUserDesignation();
	    if (bd == null) return UserDesignation.UNCLASSIFIED;
	    return UserDesignation.valueOf(bd.getDesignationKey());
    }

	@Override
    public String getUserEvaluation(BugInstance b) {
    	BugDesignation bd = b.getUserDesignation();
  	    if (bd == null) return "";
  	    return bd.getAnnotationText();
    }

	@Override
    public long getUserTimestamp(BugInstance b) {
    	BugDesignation bd = b.getUserDesignation();
  	    if (bd == null) return Long.MAX_VALUE;
  	    return bd.getTimestamp();
    }

	@Override
    public void setMode(Mode m) {
	    // TODO Auto-generated method stub
    }

  
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
	    // TODO Auto-generated method stub
	    
    }

    public void bugFiled(BugInstance b, Object bugLink) {
    	 throw new UnsupportedOperationException();
	    
    }

    public BugDesignation getPrimaryDesignation(BugInstance b) {
    	return  b.getUserDesignation();
    }

    
    protected Iterable<BugDesignation> getLatestDesignationFromEachUser(BugInstance bd) {
	    return Collections.emptyList();
    }

    public Collection<String> getProjects(String className) {
	    return Collections.emptyList();
    }

    public String getCloudName() {
        return "local storage cloud";
    }
}
