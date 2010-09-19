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
package de.tobject.findbugs.decorator.test;

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.decorators.ResourceBugCountDecorator;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the ResourceBugCountDecorator.
 * 
 * @author Tomás Pollak
 */
public class LabelDecoratorTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private static final String SOME_LABEL = "label";

    @Test
    public void testDecorateResourcesWithBugs() throws CoreException {
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Class 'A' has no visible bugs
        doTestDecoratorWithoutBugs(getClassA());

        // Class 'B' has visible bugs
        doTestDecoratorWithBugs(getClassB(), getClassBExpectedBugCount());

        // Default Java package
        doTestDecoratorWithBugs(getDefaultPackageInSrc(), getVisibleBugsCount());

        // Project
        doTestDecoratorWithBugs(getProject(), getClassBExpectedBugCount());
    }

    @Test
    public void testDecorateResourcesWithoutBugs() throws JavaModelException {
        // Class 'A'
        doTestDecoratorWithoutBugs(getClassA());

        // Class 'B'
        doTestDecoratorWithoutBugs(getClassB());

        // Default Java package
        doTestDecoratorWithoutBugs(getDefaultPackageInSrc());

        // Project
        doTestDecoratorWithoutBugs(getProject());
    }

    private void doTestDecoratorWithBugs(Object resourceAdaptable, int expectedBugsCount) {
        ResourceBugCountDecorator decorator = new ResourceBugCountDecorator();
        String decoratedText = decorator.decorateText(SOME_LABEL, resourceAdaptable);
        assertEquals(SOME_LABEL + " (" + expectedBugsCount + ")", decoratedText);
    }

    private void doTestDecoratorWithoutBugs(Object resourceAdaptable) {
        ResourceBugCountDecorator decorator = new ResourceBugCountDecorator();
        String decoratedText = decorator.decorateText(SOME_LABEL, resourceAdaptable);
        assertEquals(SOME_LABEL, decoratedText);
    }

    private int getClassBExpectedBugCount() {
        // Currently class 'B' has all the reported visible bugs
        return getVisibleBugsCount();
    }

}
