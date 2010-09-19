/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.builder.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.builder.ResourceUtils;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the public methods for ResourceUtils.
 * 
 * @author Tomás Pollak
 */
public class ResourceUtilsTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testGetResourcesPerProject_selectedClasses() throws JavaModelException {
        // Select classes A and B
        List<ICompilationUnit> classes = Arrays.asList(getClassA(), getClassB());
        Map<IProject, List<WorkItem>> resourcesPerProject = ResourceUtils
                .getResourcesPerProject(new StructuredSelection(classes));

        // We should have project -> [A.java, B.java]
        assertNotNull(resourcesPerProject);
        assertEquals(1, resourcesPerProject.size());
        assertTrue(resourcesPerProject.containsKey(getProject()));
        List<WorkItem> expectedResources = Arrays.asList(new WorkItem(getClassA()), new WorkItem(getClassB()));
        assertEquals(expectedResources, resourcesPerProject.get(getProject()));
    }

    @Test
    public void testGetResourcesPerProject_selectedProject() {
        // Select the project
        Map<IProject, List<WorkItem>> resourcesPerProject = ResourceUtils.getResourcesPerProject(new StructuredSelection(
                getProject()));

        // We should have project -> [project]
        assertNotNull(resourcesPerProject);
        assertEquals(1, resourcesPerProject.size());
        assertTrue(resourcesPerProject.containsKey(getProject()));
        assertEquals(Collections.singletonList(new WorkItem(getProject())), resourcesPerProject.get(getProject()));
    }

    @Test
    public void testGetResourcesPerProject_selectedProjectAndClasses() throws JavaModelException {
        // Select project and classes A and B
        List<?> classes = Arrays.asList(getProject(), getClassA(), getClassB());
        Map<IProject, List<WorkItem>> resourcesPerProject = ResourceUtils
                .getResourcesPerProject(new StructuredSelection(classes));

        // We should have project -> [project]
        assertNotNull(resourcesPerProject);
        assertEquals(1, resourcesPerProject.size());
        assertTrue(resourcesPerProject.containsKey(getProject()));
        assertEquals(Collections.singletonList(new WorkItem(getProject())), resourcesPerProject.get(getProject()));
    }

}
