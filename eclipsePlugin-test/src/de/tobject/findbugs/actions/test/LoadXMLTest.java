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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.junit.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.test.AbstractFindBugsTest;

/**
 * This class tests the LoadXMLAction.
 * 
 * @author tpollak
 */
public class LoadXMLTest extends AbstractFindBugsTest {

	@Test
	public void testLoadXML() throws CoreException {
		assertNoBugs();

		StructuredSelection selection = new StructuredSelection(getProject());
		IActionDelegate action = new MockLoadXMLAction(getBugsFileLocation());
		action.selectionChanged(null, selection);
		action.run(null);

		joinJobFamily(FindbugsPlugin.class);

		assertExpectedBugs();
	}
}
