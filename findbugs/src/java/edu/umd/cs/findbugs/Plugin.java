/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A FindBugs plugin.
 * A plugin contains executable Detector classes, as well as meta
 * information decribing those detectors (such as human-readable
 * detector and bug descriptions).
 *
 * @see PluginLoader
 * @author David Hovemeyer
 */
public class Plugin {
	private String pluginId;
	private String provider;
	private String website;
	private String shortDescription;
	private ArrayList<DetectorFactory> detectorFactoryList;
	private ArrayList<BugPattern> bugPatternList;
	private ArrayList<BugCode> bugCodeList;
	private boolean enabled;

	// Ordering constraints
	private ArrayList<DetectorOrderingConstraint> interPassConstraintList;
	private ArrayList<DetectorOrderingConstraint> intraPassConstraintList;

	/**
	 * Constructor.
	 * Creates an empty plugin object.
	 *
	 * @param pluginId the plugin's unique identifier
	 */
	public Plugin(String pluginId) {
		this.pluginId = pluginId;
		this.detectorFactoryList = new ArrayList<DetectorFactory>();
		this.bugPatternList = new ArrayList<BugPattern>();
		this.bugCodeList = new ArrayList<BugCode>();
		this.interPassConstraintList = new ArrayList<DetectorOrderingConstraint>();
		this.intraPassConstraintList = new ArrayList<DetectorOrderingConstraint>();
	}

	/**
	 * Set whether or not this Plugin is enabled.
	 *
	 * @param enabled true if the Plugin is enabled, false if not
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Return whether or not the Plugin is enabled.
	 *
	 * @return true if the Plugin is enabled, false if not
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set plugin provider.
	 *
	 * @param provider the plugin provider
	 */
	public void setProvider(String provider) {
		this.provider = provider;
	}

	/**
	 * Get the plugin provider.
	 *
	 * @return the provider, or null if the provider was not specified
	 */
	public String getProvider() {
		return provider;
	}

	/**
	 * Set plugin website.
	 *
	 * @param website the plugin website
	 */
	public void setWebsite(String website) {
		this.website = website;
	}

	/**
	 * Get the plugin website.
	 *
	 * @return the website, or null if the was not specified
	 */
	public String getWebsite() {
		return website;
	}

	/**
	 * Set plugin short (one-line) text description.
	 *
	 * @param the plugin short text description
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * Get the plugin short (one-line) description.
	 *
	 * @return the short description, or null if the 
	 *         short description was not specified
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Add a DetectorFactory for a Detector implemented by the Plugin.
	 * 
	 * @param factory the DetectorFactory
	 */
	public void addDetectorFactory(DetectorFactory factory) {
		detectorFactoryList.add(factory);
	}
	
	/**
	 * Add a BugPattern reported by the Plugin.
	 * 
	 * @param bugPattern
	 */
	public void addBugPattern(BugPattern bugPattern) {
		bugPatternList.add(bugPattern);
	}
	
	/**
	 * Add a BugCode reported by the Plugin.
	 * 
	 * @param bugCode
	 */
	public void addBugCode(BugCode bugCode) {
		bugCodeList.add(bugCode);
	}

	/**
	 * Add an inter-pass Detector ordering constraint.
	 *
	 * @param constraint the inter-pass Detector ordering constraint
	 */
	public void addInterPassOrderingConstraint(DetectorOrderingConstraint constraint) {
		interPassConstraintList.add(constraint);
	}

	/**
	 * Add an intra-pass Detector ordering constraint.
	 *
	 * @param constraint the intra-pass Detector ordering constraint
	 */
	public void addIntraPassOrderingConstraint(DetectorOrderingConstraint constraint) {
		intraPassConstraintList.add(constraint);
	}
	
	/**
	 * Get Iterator over DetectorFactory objects in the Plugin.
	 * 
	 * @return Iterator over DetectorFactory objects
	 */
	public Iterator<DetectorFactory> detectorFactoryIterator() {
		return detectorFactoryList.iterator();
	}
	
	/**
	 * Get Iterator over BugPattern objects in the Plugin.
	 * 
	 * @return Iterator over BugPattern objects
	 */
	public Iterator<BugPattern> bugPatternIterator() {
		return bugPatternList.iterator();
	}
	
	/**
	 * Get Iterator over BugCode objects in the Plugin.
	 * 
	 * @return Iterator over BugCode objects
	 */
	public Iterator<BugCode> bugCodeIterator() {
		return bugCodeList.iterator();
	}

	/**
	 * Return an Iterator over the inter-pass Detector ordering constraints.
	 */
	public Iterator<DetectorOrderingConstraint> interPassDetectorOrderingConstraintIterator() {
		return interPassConstraintList.iterator();
	}

	/**
	 * Return an Iterator over the intra-pass Detector ordering constraints.
	 */
	public Iterator<DetectorOrderingConstraint> intraPassDetectorOrderingConstraintIterator() {
		return intraPassConstraintList.iterator();
	}
	
	/**
	 * @return Returns the pluginId.
	 */
	public String getPluginId() {
		return pluginId;
	}
}

// vim:ts=4
