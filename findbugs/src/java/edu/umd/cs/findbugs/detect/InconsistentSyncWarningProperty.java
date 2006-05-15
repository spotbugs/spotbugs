/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.AbstractWarningProperty;
import edu.umd.cs.findbugs.props.PriorityAdjustment;

/**
 * Warning properties for inconsistent synchronization detector.
 * 
 * @author David Hovemeyer
 */
public class InconsistentSyncWarningProperty extends AbstractWarningProperty {
	private InconsistentSyncWarningProperty(String name, PriorityAdjustment priorityAdjustment) {
		super(name, priorityAdjustment);
	}
	
//	/** Field is never accessed while locked. */
//	public static final InconsistentSyncWarningProperty NEVER_LOCKED =
//		new InconsistentSyncWarningProperty("NEVER_LOCKED", PriorityAdjustment.FALSE_POSITIVE);
//	/** Field is never accessed while unlocked. */
//	public static final InconsistentSyncWarningProperty NEVER_UNLOCKED =
//		new InconsistentSyncWarningProperty("NEVER_UNLOCKED", PriorityAdjustment.FALSE_POSITIVE);
	/**
	 * Field is accessed unlocked most of the time, and therefore is probably
	 * not intended to be safely used from multiple threads.
	 */
	public static final InconsistentSyncWarningProperty MANY_BIASED_UNLOCKED =
		new InconsistentSyncWarningProperty("MANY_BIASED_UNLOCKED", PriorityAdjustment.FALSE_POSITIVE);
	/** Field is never written outside constructor. */
	public static final InconsistentSyncWarningProperty NEVER_WRITTEN =
		new InconsistentSyncWarningProperty("NEVER_WRITTEN", PriorityAdjustment.FALSE_POSITIVE);
	/** Field is never read outside constructor. */
	public static final InconsistentSyncWarningProperty NEVER_READ =
		new InconsistentSyncWarningProperty("NEVER_READ", PriorityAdjustment.FALSE_POSITIVE);
	/**
	 * Field is never locked in the definition of the class.  (I.e., all locked
	 * accesses are in methods of other classes.)
	 */
	public static final InconsistentSyncWarningProperty NO_LOCAL_LOCKS =
		new InconsistentSyncWarningProperty("NO_LOCAL_LOCKS", PriorityAdjustment.FALSE_POSITIVE);
	/** Below minimum percentage synchronized accesses. */
	public static final InconsistentSyncWarningProperty BELOW_MIN_SYNC_PERCENT =
		new InconsistentSyncWarningProperty("BELOW_MIN_SYNC_PERCENT", PriorityAdjustment.FALSE_POSITIVE);
	/** The only unlocked accesses are in getter methods. */
	public static final InconsistentSyncWarningProperty ONLY_UNSYNC_IN_GETTERS =
		new InconsistentSyncWarningProperty("ONLY_UNSYNC_IN_GETTERS", PriorityAdjustment.LOWER_PRIORITY);
	public static final InconsistentSyncWarningProperty ANNOTATED_AS_GUARDED_BY_THIS =
		new InconsistentSyncWarningProperty("ANNOTATED_AS_GUARDED_BY_THIS", PriorityAdjustment.RAISE_PRIORITY_TO_AT_LEAST_NORMAL);
	public static final InconsistentSyncWarningProperty ANNOTATED_AS_THREAD_SAFE =
		new InconsistentSyncWarningProperty("ANNOTATED_AS_THREAD_SAFE", PriorityAdjustment.RAISE_PRIORITY);

}
