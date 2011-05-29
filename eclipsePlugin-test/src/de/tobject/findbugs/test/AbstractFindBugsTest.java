/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom�s Pollak
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
package de.tobject.findbugs.test;

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Base class for the default test scenario of the FindBugs UI tests.
 *
 * @author Tom�s Pollak
 */
public abstract class AbstractFindBugsTest extends AbstractPluginTest {

    protected static final String BUGS_XML_FILE = "/src/bugs.xml";

    protected static final String FILTER_FILE = "/src/filter.xml";

    /**
     * Returns the bug file path of the test project.
     *
     * @return The absolute filesystem path of the bugs file.
     */
    protected String getBugsFileLocation() {
        IResource bugsFile = getProject().findMember(BUGS_XML_FILE);
        return bugsFile.getLocation().toOSString();
    }

    protected ICompilationUnit getClassA() throws JavaModelException {
        ICompilationUnit compilationUnit = (ICompilationUnit) getJavaProject().findElement(new Path("A.java"));
        return compilationUnit;
    }

    protected ICompilationUnit getClassB() throws JavaModelException {
        ICompilationUnit compilationUnit = (ICompilationUnit) getJavaProject().findElement(new Path("B.java"));
        return compilationUnit;
    }

    protected IPackageFragment getDefaultPackageInSrc() throws JavaModelException {
        IPackageFragment fragment = getJavaProject().findPackageFragment(
                new Path("/" + AbstractPluginTest.TEST_PROJECT + "/" + AbstractPluginTest.SRC));
        return fragment;
    }

    /**
     * Returns the filter file path of the test project.
     *
     * @return The absolute path of the filter file.
     */
    protected String getFilterFileLocation() {
        IResource filterFile = getProject().findMember(FILTER_FILE);
        return filterFile.getLocation().toOSString();
    }

    @Override
    protected TestScenario getTestScenario() {
        return TestScenario.DEFAULT;
    }

    /**
     * Configures the test project to use the baseline bugs file.
     *
     * @param b
     */
    protected void setBaselineBugsFile(boolean on) throws CoreException {
        // per default, workspace settings are used. We enable project settings
        // here
        FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
        UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject());
        HashMap<String, Boolean> map = new HashMap<String, Boolean>();
        if (on) {
            map.put(getBugsFileLocation(), Boolean.TRUE);
            preferences.setExcludeBugsFiles(map);
        } else {
            preferences.setExcludeBugsFiles(map);
        }
        FindbugsPlugin.saveUserPreferences(getProject(), preferences);
    }

    /**
     * Configures the test project to use the filter file.
     */
    protected void setFilterFile(boolean on) throws CoreException {
        // per default, workspace settings are used. We enable project settings
        // here
        FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
        UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject());
        HashMap<String, Boolean> map = new HashMap<String, Boolean>();
        if (on) {
            map.put(getFilterFileLocation(), Boolean.TRUE);
            preferences.setExcludeFilterFiles(map);
        } else {
            preferences.setExcludeFilterFiles(new HashMap<String, Boolean>());
        }
        FindbugsPlugin.saveUserPreferences(getProject(), preferences);
    }

}
