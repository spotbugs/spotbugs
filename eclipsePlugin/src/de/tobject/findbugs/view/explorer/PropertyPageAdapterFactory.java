/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.view.explorer;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class PropertyPageAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		// return nothing to get rid of errors generated through
		// TabbedPropertySheetAdapterFactory which is adapting to CommonNavigator class
		return null;
	}

	public Class[] getAdapterList() {
        return new Class[] {IPropertySheetPage.class};
	}

}
