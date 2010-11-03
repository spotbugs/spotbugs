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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Input/output helper methods.
 *
 * @author David Hovemeyer
 */
public abstract class IO {
    /**
     * Write the contents of a file in the Eclipse workspace.
     *
     * @param file
     *            the file to write to
     * @param output
     *            the FileOutput object responsible for generating the data
     * @param monitor
     *            a progress monitor (or null if none)
     * @throws CoreException
     */
    public static void writeFile(IFile file, final FileOutput output, IProgressMonitor monitor) throws CoreException {

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            output.writeFile(bos);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            if (!file.exists()) {
                mkdirs(file, monitor);
                file.create(bis, true, monitor);
            } else {
                file.setContents(bis, true, false, monitor);
            }
        } catch (IOException e) {
            IStatus status = FindbugsPlugin.createErrorStatus("Exception while " + output.getTaskDescription(), e);
            throw new CoreException(status);
        }
    }

    /**
     * Recursively creates all folders needed, up to the project. Project must
     * already exist.
     *
     * @param resource
     *            non null
     * @param monitor
     *            non null
     * @throws CoreException
     */
    private static void mkdirs(@Nonnull IResource resource, IProgressMonitor monitor) throws CoreException {
        IContainer container = resource.getParent();
        if (container.getType() == IResource.FOLDER && !container.exists()) {
            if(!container.getParent().exists()) {
                mkdirs(container, monitor);
            }
            ((IFolder) container).create(true, true, monitor);
        }
    }

    /**
     * Write the contents of a java.io.File
     *
     * @param file
     *            the file to write to
     * @param output
     *            the FileOutput object responsible for generating the data
     */
    public static void writeFile(final File file, final FileOutput output, final IProgressMonitor monitor) throws CoreException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            if (monitor != null) {
                monitor.subTask("writing data to " + file.getName());
            }
            output.writeFile(bout);
            bout.flush();
        } catch (IOException e) {
            IStatus status = FindbugsPlugin.createErrorStatus("Exception while " + output.getTaskDescription(), e);
            throw new CoreException(status);
        } finally {
            closeQuietly(fout);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
