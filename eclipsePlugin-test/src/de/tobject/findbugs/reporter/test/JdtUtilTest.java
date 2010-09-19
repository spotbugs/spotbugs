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
package de.tobject.findbugs.reporter.test;

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.reporter.JdtUtils;
import de.tobject.findbugs.test.AbstractPluginTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the JdtUtil class.
 * 
 * @author Tomás Pollak
 */
public class JdtUtilTest extends AbstractPluginTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.JDT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testFindAnonymous() throws JavaModelException {
        IType typeC = getTypeC();
        IType typeE = getTypeE();

        // Positive case: Runnable in C
        doPositiveTest(typeC, "1");

        // Positive case: Cloneable in C
        doPositiveTest(typeC, "2");

        // Positive case: Comparator in E
        doPositiveTest(typeE, "1");

        // Negative case: Low boundary
        doNullTest(typeC, "0");

        // Negative case: High boundary
        doNullTest(typeC, "3");

        // Negative case: Null argument
        try {
            JdtUtils.findAnonymous(typeC, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }

        // Negative case: Not integer
        doNullTest(typeC, "bla");
    }

    @Override
    protected TestScenario getTestScenario() {
        return TestScenario.JDT;
    }

    protected IType getTypeC() throws JavaModelException {
        IType type = getJavaProject().findType("C");
        return type;
    }

    protected IType getTypeE() throws JavaModelException {
        IType type = getJavaProject().findType("C.E");
        return type;
    }

    private void doNullTest(IType parentType, String anonymousClassNumber) {
        IType typeZero = JdtUtils.findAnonymous(parentType, anonymousClassNumber);
        assertNull(typeZero);
    }

    private void doPositiveTest(IType parentType, String anonymousClassNumber) {
        IType childType = JdtUtils.findAnonymous(parentType, anonymousClassNumber);
        assertNotNull(childType);
        assertTrue(childType.exists());
        assertEquals("", childType.getElementName());
    }

}
