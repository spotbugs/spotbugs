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

package edu.umd.cs.findbugs.bugReporter;

import java.lang.reflect.Constructor;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.DelegatingBugReporter;

/**
 * Abstract base class for bug reporters defined as plugins.
 *
 * @author pugh
 */
public abstract class BugReporterDecorator extends DelegatingBugReporter {

    static public BugReporterDecorator construct(ComponentPlugin<BugReporterDecorator> plugin, BugReporter delegate) {

        Class<? extends BugReporterDecorator> pluginClass = plugin.getComponentClass();

        try {
            Constructor<? extends BugReporterDecorator> constructor = pluginClass.getConstructor(ComponentPlugin.class,
                    BugReporter.class);
            return constructor.newInstance(plugin, delegate);
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getCause());

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to construct " + plugin.getId(), e);
        }

    }

    final ComponentPlugin<BugReporterDecorator> plugin;

    public BugReporterDecorator(ComponentPlugin<BugReporterDecorator> plugin, BugReporter delegate) {
        super(delegate);
        this.plugin = plugin;

    }

}
