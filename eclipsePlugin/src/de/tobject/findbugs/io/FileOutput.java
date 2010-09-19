/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2005, University of Maryland
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
package de.tobject.findbugs.io;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface for an object responsible for generating data to use when writing
 * the contents of a file in the Eclipse workspace.
 * 
 * @see de.tobject.findbugs.io.IO#writeFile(IFile, FileOutput, IProgressMonitor)
 * @author David Hovemeyer
 */
public interface FileOutput {
    /**
     * Write data to file.
     * 
     * @param os
     *            the OutputStream for the file
     * @throws IOException
     */
    public void writeFile(OutputStream os) throws IOException;

    /**
     * Get a description of the task. E.g., "writing saved XML bug data".
     * 
     * @return description of the task
     */
    public String getTaskDescription();
}
