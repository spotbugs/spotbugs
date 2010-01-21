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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.cloud.username.NameLookup;
import edu.umd.cs.findbugs.cloud.username.PromptForNameLookup;


/**
 * @author pwilliam
 */
public class CloudFactory {
	
	public static boolean DEBUG = false;
	
    private static final String DEFAULT_CLOUD_CLASS = "edu.umd.cs.findbugs.cloud.db.DBCloud";

    
    public static NameLookup getNameLookup(BugCollection bc) {
    	String cloudClassName = SystemProperties.getProperty("findbugs.namelookup.classname");
    	Class <? extends NameLookup> c = null;
    	if (cloudClassName != null) try {
    		c = Class.forName(cloudClassName).asSubclass(NameLookup.class);
    	} catch (ClassNotFoundException e) {
	       AnalysisContext.logError("Unable to load " + cloudClassName, e);
        }
    	if (c == null)
    		c = PromptForNameLookup.class;
    	NameLookup result = null;
        try {
	        result = c.newInstance();
        } catch (Exception e) {
            AnalysisContext.logError("Unable to construct " + cloudClassName, e);
        }
       
    	if (result == null || !result.init(bc)) {
    		result = new PromptForNameLookup();
        	if (!result.init(bc))
        		throw new AssertionError("Can't init prompt for name lookup");
    	}
    	
    	return result;
		
    }

    private static Class<? extends Cloud> getCloudClass() throws ClassNotFoundException {
    	String cloudClassName = SystemProperties.getProperty("findbugs.cloud.classname");
		boolean cloudClassSpecified = cloudClassName != null;
		if (!cloudClassSpecified)
			cloudClassName = DEFAULT_CLOUD_CLASS;
		Class<? extends Cloud> cloudClass = registeredClouds.get(cloudClassName);
		if (cloudClass == null)
			cloudClass = Class.forName(cloudClassName).asSubclass(Cloud.class);
      
		return cloudClass;
    }
    
    private static Map<String, Class<? extends Cloud>> registeredClouds = new HashMap<String, Class<? extends Cloud>> ();
    
    public static void addCloud(Class<? extends Cloud> cloudClass) {
    	registeredClouds.put(cloudClass.getName(), cloudClass);
    }
	public static Cloud getCloud(BugCollection bc) {
		try {
			Class<? extends Cloud> cloudClass = getCloudClass();
			String cloudClassName = cloudClass.getName();
	        Constructor<? extends Cloud> constructor = cloudClass.getConstructor(BugCollection.class);
			Cloud cloud = constructor.newInstance(bc);
			if (DEBUG)
				bc.getProject().getGuiCallback().showMessageDialog("constructed " + cloudClassName);
			if (cloud.availableForInitialization()) {
				if (DEBUG)
					bc.getProject().getGuiCallback().showMessageDialog("attempting to initialize " + cloudClassName);
				
				if (cloud.initialize())
					return cloud;
				bc.getProject().getGuiCallback().showMessageDialog("Unable to connect to " + cloudClass.getSimpleName());
				if (SystemProperties.getBoolean("findbugs.failIfUnableToConnectToDB"))
					System.exit(1);
			} 
		} catch (ClassNotFoundException e) {
			assert true;
        } catch (Exception e) {
	      assert true;
        }
        
        return getPlainCloud(bc);
	}


    public static Cloud getPlainCloud(BugCollection bc) {
	    Cloud cloud = new BugCollectionStorageCloud(bc);
        cloud.initialize();
        return cloud;
    }
	
}
