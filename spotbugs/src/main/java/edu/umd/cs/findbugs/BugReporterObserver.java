/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

/**
 * Observer to determine when a BugReporter reports a bug. By adding an
 * observer, it is possible to determine which bugs a BugReporter is actually
 * reporting, because due to filtering, priorities, etc., not all bugs sent to a
 * BugReporter will actually be processed.
 *
 * @author David Hovemeyer
 * @see BugReporter
 */
public interface BugReporterObserver {
    /**
     * Called when a BugReporter reports a bug.
     *
     * @param bugInstance
     *            the BugInstance
     */
    public void reportBug(BugInstance bugInstance);
}

