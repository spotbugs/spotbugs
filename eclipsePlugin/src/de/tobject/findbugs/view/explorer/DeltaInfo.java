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

import org.eclipse.core.resources.IResourceDelta;

class DeltaInfo{

	final Object data;

	/**
	 * @see IResourceDelta#getKind()
	 */
	final int changeKind;

	public DeltaInfo(Object element, int kind) {
		data = element;
		changeKind = kind;
	}

	@Override
	public boolean equals(Object obj) {
		if(data == null){
			return false;
		}
		if(obj instanceof DeltaInfo){
			return data.equals(((DeltaInfo)obj).data);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return data == null? 0 : data.hashCode();
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();
		switch (changeKind) {
		case IResourceDelta.ADDED:
			sb.append("ADDED");
			break;
		case IResourceDelta.CHANGED:
			sb.append("CHANGED");
			break;
		case IResourceDelta.REMOVED:
			sb.append("REMOVED");
			break;
		default:
			sb.append(" ? ").append(changeKind);
			break;
		}
		sb.append(" ").append(data);
		return sb.toString();
	}
}
