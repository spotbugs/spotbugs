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
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * @author pwilliam
 */
public class CloudFactory {

    private static final String FINDBUGS_NAMELOOKUP_CLASSNAME = "findbugs.namelookup.classname";

    private static final String FINDBUGS_NAMELOOKUP_REQUIRED = "findbugs.namelookup.required";

    public static final String FAIL_ON_CLOUD_ERROR_PROP = "findbugs.failOnCloudError";
    public static final boolean FAIL_ON_CLOUD_ERROR = SystemProperties.getBoolean(FAIL_ON_CLOUD_ERROR_PROP, false);

    public static boolean DEBUG = SystemProperties.getBoolean("findbugs.cloud.debug", false);

    public static String DEFAULT_CLOUD = SystemProperties.getProperty("findbugs.cloud.default");

    private static final Logger LOGGER = Logger.getLogger(CloudFactory.class.getName());

    public static @Nonnull Cloud createCloudWithoutInitializing(BugCollection bc) {
        CloudPlugin plugin = getCloudPlugin(bc);
        if (plugin == null) {
            LOGGER.log(Level.FINE, "default cloud " + DEFAULT_CLOUD + " not registered");
            return getPlainCloud(bc);
        }
        LOGGER.log(Level.FINE, "Using cloud plugin " + plugin.getId());

        try {
            Class<? extends Cloud> cloudClass = plugin.getCloudClass();
            Properties properties = bc.getProject().getCloudProperties();
            Constructor<? extends Cloud> constructor = cloudClass.getConstructor(CloudPlugin.class, BugCollection.class,
                    Properties.class);
            Cloud cloud = constructor.newInstance(plugin, bc, properties);
            if (DEBUG) {
                bc.getProject().getGuiCallback().showMessageDialog("constructed " + cloud.getClass().getName());
            }
            LOGGER.log(Level.FINE, "constructed cloud plugin " + plugin.getId());
            if (!cloud.availableForInitialization()) {
                handleInitializationException(bc, plugin,
                        new IllegalStateException(cloud.getClass().getName() + " cloud " + plugin.getId()+ " doesn't have information needed for initialization"));
            }
            return cloud;
        } catch (InvocationTargetException e) {
            return handleInitializationException(bc, plugin, e.getCause());
        } catch (Exception e) {
            return handleInitializationException(bc, plugin, e);
        }

    }

    public static CloudPlugin getCloudPlugin(BugCollection bc) {
        CloudPlugin plugin = null;
        Project project = bc.getProject();
        assert project != null;
        String cloudId = project.getCloudId();
        if (cloudId != null) {
            plugin = DetectorFactoryCollection.instance().getRegisteredClouds().get(cloudId);
            if (plugin == null && FAIL_ON_CLOUD_ERROR) {
                throw new IllegalArgumentException("Cannot find registered cloud for " + cloudId);
            }
        }
        // is the desired plugin disabled for this project (and/or globally)? if so, skip it.
        if (plugin != null) {
            Plugin fbplugin = Plugin.getByPluginId(plugin.getFindbugsPluginId());
            //noinspection PointlessBooleanExpression
            if (fbplugin != null && Boolean.FALSE.equals(project.getPluginStatus(fbplugin))) {
                plugin = null; // use default cloud below
            }
        }
        if (plugin == null) {
            if (DEFAULT_CLOUD != null) {
                LOGGER.log(Level.FINE, "Trying default cloud " + DEFAULT_CLOUD);
            }
            cloudId = DEFAULT_CLOUD;
            plugin = DetectorFactoryCollection.instance().getRegisteredClouds().get(cloudId);
        }
        return plugin;
    }

    public static Cloud handleInitializationException(BugCollection bc, CloudPlugin plugin, Throwable e) {
        if (DEBUG) {
            bc.getProject().getGuiCallback().showMessageDialog("failed " + e.getMessage() + e.getClass().getName());
        }
        LOGGER.log(Level.WARNING, "Could not load cloud plugin " + plugin, e);
        if (SystemProperties.getBoolean("findbugs.failIfUnableToConnectToCloud")) {
            System.exit(1);
        }
        return getPlainCloud(bc);
    }

    public static void initializeCloud(BugCollection bc, Cloud cloud) throws IOException {
        IGuiCallback callback = bc.getProject().getGuiCallback();

        if (!cloud.availableForInitialization()) {
            return;
        }

        if (DEBUG) {
            callback.showMessageDialog("attempting to initialize " + cloud.getClass().getName());
        }

        if (!cloud.initialize()) {
            throw new IOException("Unable to connect to " + cloud.getCloudName());
        }

        if (DEBUG) {
            callback.showMessageDialog("initialized " + cloud.getClass().getName());
        }
    }


    public static @Nonnull Cloud getPlainCloud(BugCollection bc) {
        DoNothingCloud cloud = new DoNothingCloud(bc);
        if (cloud.initialize()) {
            return cloud;
        }
        throw new IllegalStateException("Unable to initialize DoNothingCloud");
    }

}
