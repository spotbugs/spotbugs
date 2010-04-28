/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Alessandro Nistico
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
package de.tobject.findbugs.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.synchronize.ChangeSetDiffNode;

/**
 * Adapter factory to allow enablement of findbugs on change sets
 *
 * @author Alex
 *
 */
public class ChangeSetAdapterFactory implements IAdapterFactory {

	/**
	 * {@inheritDoc}
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ChangeSetDiffNode
				&& adapterType == ChangeSet.class) {
			ChangeSetDiffNode node = (ChangeSetDiffNode) adaptableObject;
			return node.getSet();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class[] getAdapterList() {
		return new Class[] { ChangeSet.class };
	}

}
