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

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.SystemProperties;


/**
 * @author pwilliam
 */
public class CloudFactory {
	
	
    private static final String DEFAULT_CLOUD_CLASS = "edu.umd.cs.findbugs.cloud.db.DBCloud";


	public static Cloud getCloud(BugCollection bc) {
		String cloudClassName = SystemProperties.getProperty("findbugs.cloud.classname");
		boolean cloudClassSpecified = cloudClassName != null;
		if (!cloudClassSpecified)
			cloudClassName = DEFAULT_CLOUD_CLASS;
		try {
	        Class<? extends Cloud> cloudClass 
	        = Class.forName(cloudClassName).asSubclass(Cloud.class);
	        Constructor<? extends Cloud> constructor = cloudClass.getConstructor(BugCollection.class);
			Cloud cloud = constructor.newInstance(bc);
			if (cloud.availableForInitialization()) {
				if (cloud.initialize())
					return cloud;
				bc.getProject().getGuiCallback().showMessageDialog("Unable to connect to " + cloudClass.getSimpleName());
				if (SystemProperties.getBoolean("findbugs.failIfUnableToConnectToDB"))
					System.exit(1);
			} else if (cloudClassSpecified) {
				bc.getProject().getGuiCallback().showMessageDialog("Bad configuration for " + cloudClass.getSimpleName());
			}
		} catch (ClassNotFoundException e) {
			if (cloudClassSpecified)
				bc.getProject().getGuiCallback().showMessageDialog("Unable to load cloud " + cloudClassName);
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
