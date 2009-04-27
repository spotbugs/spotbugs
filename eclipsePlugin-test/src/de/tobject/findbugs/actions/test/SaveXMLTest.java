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
package de.tobject.findbugs.actions.test;

import static org.junit.Assert.*;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.test.AbstractFindBugsTest;

/**
 * This class tests the SaveXMLAction.
 * 
 * @author Tomás Pollak
 */
public class SaveXMLTest extends AbstractFindBugsTest {

	private File tempFile;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		tempFile = File.createTempFile("bugs", ".xml");
		assertTrue(tempFile.delete());
	}

	@Override
	@After
	public void tearDown() throws CoreException {
		tempFile.delete();
		super.tearDown();
	}

	@Test
	public void testSaveXML() throws CoreException {
		assertNoBugs();
		work(createFindBugsWorker());
		assertExpectedBugs();

		StructuredSelection selection = new StructuredSelection(getProject());
		IActionDelegate action = new SaveXMLActionTestSubclass(getTempFilePath());
		action.selectionChanged(null, selection);
		action.run(null);

		joinJobFamily(FindbugsPlugin.class);

		clearBugsState();
		assertNoBugs();

		loadXml(createFindBugsWorker(), getTempFilePath());
		assertExpectedBugs();
	}

	private String getTempFilePath() {
		return tempFile.getAbsolutePath();
	}
}
