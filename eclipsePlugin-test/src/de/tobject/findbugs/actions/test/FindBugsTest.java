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
import org.junit.Test;

import de.tobject.findbugs.actions.FindBugsAction;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

/**
 * This class tests the FindBugsAction.
 * 
 * @author tpollak
 */
public class FindBugsTest extends AbstractFindBugsTest {
	@Test
	public void testRunFindBugs() throws CoreException {
		assertNoBugs();

		StructuredSelection selection = new StructuredSelection(getProject());
		FindBugsAction action = new FindBugsAction();
		action.selectionChanged(null, selection);
		action.run(null);

		joinJobFamily(MutexSchedulingRule.class);

		assertExpectedBugs();
	}
}
