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
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SystemProperties;


/**
 * @author pwilliam
 */
public class CloudFactory {
    
    private static final String FINDBUGS_NAMELOOKUP_CLASSNAME = "findbugs.namelookup.classname";
    private static final String FINDBUGS_NAMELOOKUP_REQUIRED = "findbugs.namelookup.required";

	public static boolean DEBUG = SystemProperties.getBoolean("findbugs.cloud.debug",false);
	public static String DEFAULT_CLOUD = SystemProperties.getProperty("findbugs.cloud.default");

    private static final Logger LOGGER = Logger.getLogger(CloudFactory.class.getName());


    public static Cloud createCloudWithoutInitializing(BugCollection bc) {
    		CloudPlugin plugin = null;
    		String cloudId = bc.getProject().getCloudId();
    		if (cloudId != null) {
    			plugin = registeredClouds.get(cloudId);
    		}
    		boolean usedDefaultCloud = false;
    		if (plugin == null)  {
    			if (DEFAULT_CLOUD != null)
    				 LOGGER.log(Level.FINE, "Trying default cloud " + DEFAULT_CLOUD);
    			cloudId = DEFAULT_CLOUD;
    			plugin = registeredClouds.get(cloudId);
    			usedDefaultCloud = true;
    			if (plugin == null) {
    				LOGGER.log(Level.FINE, "default cloud " + DEFAULT_CLOUD + " not registered");
    		    		
    			  return getPlainCloud(bc);
    			}
    		
    		}
    		 LOGGER.log(Level.FINE, "Using cloud plugin " + plugin.getId());;
 			
    		
		try {
			Class<? extends Cloud> cloudClass = plugin.getCloudClass();
			Properties properties = bc.getProject().getCloudProperties();
	        Constructor<? extends Cloud> constructor = cloudClass.getConstructor(CloudPlugin.class, BugCollection.class, Properties.class);
			Cloud cloud = constructor.newInstance(plugin, bc, properties);
			if (DEBUG)
				bc.getProject().getGuiCallback().showMessageDialog("constructed " + cloud.getClass().getName());
			 LOGGER.log(Level.FINE, "constructed cloud plugin " + plugin.getId());;
		 	if (false && usedDefaultCloud)
		 		bc.getProject().setCloudId(plugin.getId());
			return cloud;
		} catch (Exception e) {
			if (DEBUG) {
				bc.getProject().getGuiCallback().showMessageDialog("failed " + e.getMessage() + e.getClass().getName());
			}
            LOGGER.log(Level.WARNING, "Could not load cloud plugin " + plugin, e);
        	if (SystemProperties.getBoolean("findbugs.failIfUnableToConnectToCloud"))
    			System.exit(1);
            return getPlainCloud(bc);
        }
	
	}
	
	public static void initializeCloud(BugCollection bc, Cloud cloud) throws IOException {
        IGuiCallback callback = bc.getProject().getGuiCallback();

        if (cloud.availableForInitialization()) {
            if (DEBUG)
                callback.showMessageDialog("attempting to initialize " + cloud.getClass().getName());

            if (cloud.initialize()) {
                if (DEBUG)
                    callback.showMessageDialog("initialized " + cloud.getClass().getName());

                return;
            }
            callback.showMessageDialog("Unable to connect to " + cloud.getCloudName());

        }
	}


    public static Cloud getPlainCloud(BugCollection bc) {
	    BugCollectionStorageCloud cloud = new BugCollectionStorageCloud(bc);
        if (cloud.initialize())	return cloud;
        throw new IllegalStateException("Unable to initialize plain cloud");
    }

    static  Map<String, CloudPlugin> registeredClouds = new LinkedHashMap<String, CloudPlugin>();
    
    public static  Map<String, CloudPlugin> getRegisteredClouds() {
    		return Collections.unmodifiableMap(registeredClouds);
    }
    /**
     * @param cloudPlugin
	 * @param enabled TODO
     */
    public static void registerCloud(CloudPlugin cloudPlugin, boolean enabled) {
    	 LOGGER.log(Level.FINE, "Registering " + cloudPlugin.getId());
			
    	if (enabled) 
    		registeredClouds.put(cloudPlugin.getId(), cloudPlugin);
    }
	
}
