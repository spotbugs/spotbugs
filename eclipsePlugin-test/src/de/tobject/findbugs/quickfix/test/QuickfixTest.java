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
package de.tobject.findbugs.quickfix.test;

import java.io.IOException;

import de.tobject.findbugs.test.AbstractQuickfixTest;
import de.tobject.findbugs.test.TestScenario;

import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CreateAndOddnessCheckResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CreateRemainderOddnessCheckResolution;

import org.eclipse.core.runtime.CoreException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests the quickfix resolutions.
 *
 * @author Tom�s Pollak
 */
public class QuickfixTest extends AbstractQuickfixTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.QUICKFIX);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testChangePublicToProtectedResolution() throws CoreException, IOException {
        enableBugCategory("MALICIOUS_CODE");

        doTestQuickfixResolution("ChangePublicToProtectedResolutionExample.java", "FI_PUBLIC_SHOULD_BE_PROTECTED");
    }

    @Test
    public void testCreateAndOddnessCheckResolution() throws CoreException, IOException {
        doTestQuickfixResolution("CreateAndOddnessCheckResolutionExample.java", CreateAndOddnessCheckResolution.class,
                "IM_BAD_CHECK_FOR_ODD");
    }

    @Test
    public void testCreateDoPrivilegedBlockResolution() throws CoreException, IOException {
        doTestQuickfixResolution("CreateDoPrivilegedBlockResolutionExample.java", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED");
    }

    @Test
    public void testCreateMutableCloneResolution() throws CoreException, IOException {
        enableBugCategory("MALICIOUS_CODE");

        doTestQuickfixResolution("CreateMutableCloneResolutionExample.java", "EI_EXPOSE_REP");
    }

    @Test
    public void testCreateRemainderOddnessCheckResolution() throws CoreException, IOException {
        doTestQuickfixResolution("CreateRemainderOddnessCheckResolutionExample.java",
                CreateRemainderOddnessCheckResolution.class, "IM_BAD_CHECK_FOR_ODD");
    }

    @Test
    public void testMakeFieldFinalResolution() throws CoreException, IOException {
        enableBugCategory("MALICIOUS_CODE");

        doTestQuickfixResolution("MakeFieldFinalResolutionExample.java", "MS_SHOULD_BE_FINAL");
    }

    @Test
    public void testMakeFieldStaticResolution() throws CoreException, IOException {
        doTestQuickfixResolution("MakeFieldStaticResolutionExample.java", "SS_SHOULD_BE_STATIC");
    }

    @Test
    public void testMakeInnerTypeStaticResolution() throws CoreException, IOException {
        doTestQuickfixResolution("MakeInnerTypeStaticResolutionExample.java", "SIC_INNER_SHOULD_BE_STATIC");
    }

    @Test
    public void testRemoveUselessMethodResolution_FI_Empty() throws CoreException, IOException {
        doTestQuickfixResolution("RemoveUselessMethodResolutionFIEmptyExample.java", "FI_EMPTY");
    }

    @Test
    public void testRemoveUselessMethodResolution_FI_Useless() throws CoreException, IOException {
        doTestQuickfixResolution("RemoveUselessMethodResolutionFIUselessExample.java", "FI_USELESS");
    }

    @Test
    public void testRemoveUselessStatementResolution() throws CoreException, IOException {
        doTestQuickfixResolution("RemoveUselessStatementResolutionExample.java", "ESync_EMPTY_SYNC");
    }

    @Test
    public void testUseEqualsResolution() throws CoreException, IOException {
        getProjectPreferences().getFilterSettings().setMinPriority("Low");
        QuickFixTestPackager pack = new QuickFixTestPackager();
        pack.addBugPatterns("ES_COMPARING_STRINGS_WITH_EQ", "ES_COMPARING_PARAMETER_STRING_WITH_EQ");
        pack.addExpectedLines(5, 11);
        pack.setExpectedLabels(0, "Use equals(...) instead");
        pack.setExpectedLabels(1, "Use equals(...) instead");
        doTestQuickfixResolution("UseEqualsResolutionExample.java", pack.asList());
    }


    @Test
    public void testUseValueOfResolution() throws CoreException, IOException {
        QuickFixTestPackager pack = new QuickFixTestPackager();
        pack.addBugPatterns("DM_BOOLEAN_CTOR", "DM_NUMBER_CTOR");
        pack.addExpectedLines(3, 7);
        pack.setExpectedLabels(0, "Use Boolean.valueOf(value) instead");
        pack.setExpectedLabels(1, "Use Integer.valueOf(value) instead");
        doTestQuickfixResolution("UseValueOfResolutionExample.java", pack.asList());
    }

    @Override
    protected String getOutputFolderName()  {
        return "/quickfixOutput/";
    }



}




