/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.builder;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IFile;

/**
 * The <code>NullFilesCollector</code> is a security stand-in for the case that
 * no other files collector can be found by the
 * {@link de.tobject.findbugs.builder.FilesCollectorFactory}. It will always
 * return an empty list of files.
 *
 * @pattern NullObject
 * @author Peter Friese
 * @version 1.0
 * @since 26.09.2003
 */
public class NullFilesCollector extends AbstractFilesCollector {

	/* (non-Javadoc)
	 * @see de.tobject.findbugs.builder.AbstractFilesCollector#getFiles()
	 */
	@Override
	public Collection<IFile> getFiles() {
		return new ArrayList<IFile>();
	}

}
