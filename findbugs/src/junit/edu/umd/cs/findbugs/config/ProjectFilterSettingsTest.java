/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.config;

import edu.umd.cs.findbugs.I18N;

import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ProjectFilterSettingsTest extends TestCase {
	ProjectFilterSettings plain;

	protected void setUp() {
		plain = ProjectFilterSettings.createDefault();
	}

	public void testPlainPrio() {
		Assert.assertTrue(plain.getMinPriority().equals(ProjectFilterSettings.DEFAULT_PRIORITY));
	}

	public void testPlainCategories() {
		int count = 0;
		Iterator<String> i = I18N.instance().getBugCategories().iterator();
		while (i.hasNext()) {
			String category = i.next();
			Assert.assertTrue(plain.containsCategory(category));
			++count;
		}
		Assert.assertTrue(plain.getActiveCategorySet().size() == count);
	}
}

// vim:ts=4
