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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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
	 * @param file    the file to write to
	 * @param output  the FileOutput object responsible for generating the data
	 * @param monitor a progress monitor (or null if none)
	 * @throws IOException
	 * @throws CoreException
	 */
	public static void writeFile(
			IFile file, final FileOutput output, IProgressMonitor monitor)
			throws IOException, CoreException {
		PipedInputStream pin = new PipedInputStream();
		final PipedOutputStream pout = new PipedOutputStream();
		
		// Create a thread to write bug collection to output stream
		Thread worker = new Thread() {
			public void run() {
				try {
					//bugCollection.writeXML(pout, findbugsProject);
					output.writeFile(pout);
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(
							e, "Exception while " + output.getTaskDescription());
				} finally {
					try {
						pout.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		};		
		
		try {
			pin.connect(pout);
			worker.start();			
			
			if (!file.exists())
				file.create(pin, true, monitor);
			else
				file.setContents(pin, true, false, monitor);
			
			// Need to refresh here?
			file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} finally {
			try {
				pin.close();
			} catch (IOException e) {
				// ignore
			}
		}
		
	}

	/**
	 * Write the contents of a java.io.File
	 * 
	 * @param file    the file to write to
	 * @param output  the FileOutput object responsible for generating the data
	 */
	public static void writeFile(
			final File file, final FileOutput output, final IProgressMonitor monitor) {
		FileOutputStream fout=null;
		try {
			fout = new FileOutputStream(file);
			BufferedOutputStream bout = new BufferedOutputStream(fout);
			if (monitor!=null) monitor.subTask("writing data to "+file.getName());
			output.writeFile(bout);
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(
					e, "Exception while " + output.getTaskDescription());
		} finally {
			try {
				if (fout!=null) fout.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

}
