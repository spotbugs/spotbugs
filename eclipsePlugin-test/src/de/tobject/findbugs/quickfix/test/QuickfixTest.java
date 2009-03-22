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

import org.eclipse.core.runtime.CoreException;
import org.junit.Ignore;
import org.junit.Test;

import de.tobject.findbugs.test.AbstractQuickfixTest;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CreateAndOddnessCheckResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CreateRemainderOddnessCheckResolution;

/**
 * This class tests the quickfix resolutions.
 * 
 * @author Tomás Pollak
 */
@Ignore
public class QuickfixTest extends AbstractQuickfixTest {
	@Test
	public void testChangePublicToProtectedResolution() throws CoreException, IOException {
		enableBugCategory("MALICIOUS_CODE");

		doTestQuickfixResolution("ChangePublicToProtectedResolutionExample.java",
				"FI_PUBLIC_SHOULD_BE_PROTECTED");
	}

	@Test
	public void testCreateAndOddnessCheckResolution() throws CoreException, IOException {
		doTestQuickfixResolution("CreateAndOddnessCheckResolutionExample.java",
				CreateAndOddnessCheckResolution.class, "IM_BAD_CHECK_FOR_ODD");
	}

	@Test
	public void testCreateDoPrivilegedBlockResolution() throws CoreException, IOException {
		doTestQuickfixResolution("CreateDoPrivilegedBlockResolutionExample.java",
				"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED");
	}

	@Test
	public void testCreateMutableCloneResolution() throws CoreException, IOException {
		enableBugCategory("MALICIOUS_CODE");

		doTestQuickfixResolution("CreateMutableCloneResolutionExample.java",
				"EI_EXPOSE_REP");
	}

	@Test
	public void testCreateRemainderOddnessCheckResolution() throws CoreException,
			IOException {
		doTestQuickfixResolution("CreateRemainderOddnessCheckResolutionExample.java",
				CreateRemainderOddnessCheckResolution.class, "IM_BAD_CHECK_FOR_ODD");
	}

	@Test
	public void testMakeFieldFinalResolution() throws CoreException, IOException {
		enableBugCategory("MALICIOUS_CODE");

		doTestQuickfixResolution("MakeFieldFinalResolutionExample.java",
				"MS_SHOULD_BE_FINAL");
	}

	@Test
	public void testMakeFieldStaticResolution() throws CoreException, IOException {
		doTestQuickfixResolution("MakeFieldStaticResolutionExample.java",
				"SS_SHOULD_BE_STATIC");
	}

	@Test
	public void testMakeInnerTypeStaticResolution() throws CoreException, IOException {
		doTestQuickfixResolution("MakeInnerTypeStaticResolutionExample.java",
				"SIC_INNER_SHOULD_BE_STATIC");
	}

	@Test
	public void testUseValueOfResolution() throws CoreException, IOException {
		doTestQuickfixResolution("UseValueOfResolutionExample.java", "DM_BOOLEAN_CTOR",
				"DM_NUMBER_CTOR");
	}

}
