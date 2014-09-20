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
package de.tobject.findbugs.quickfix.test;

import java.io.IOException;

import de.tobject.findbugs.test.AbstractQuickfixTest;
import de.tobject.findbugs.test.TestScenario;

import org.eclipse.core.runtime.CoreException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests the quickfix resolutions for examples that use JUnit.
 *
 * @author Tomás Pollak
 */
public class QuickfixWithJUnitTest extends AbstractQuickfixTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.QUICKFIX_WITH_JUNIT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testCreateSuperCallResolution() throws CoreException, IOException {
        doTestQuickfixResolution("CreateSuperCallResolutionExample.java", "FI_MISSING_SUPER_CALL", "IJU_SETUP_NO_SUPER",
                "IJU_TEARDOWN_NO_SUPER");
    }

    @Override
    protected TestScenario getTestScenario() {
        return TestScenario.QUICKFIX_WITH_JUNIT;
    }

    @Override
    protected String getOutputFolderName()  {
        return "/quickfixOutput/";
    }
}
