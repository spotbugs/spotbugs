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

package edu.umd.cs.findbugs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

/**
 * A work-alike class to use instead of BCEL's ClassPath class.
 * The main difference is that URLClassPath can load
 * classfiles from URLs.
 * 
 * @author David Hovemeyer
 */
public class URLClassPath implements Serializable {
	/**
	 * Interface describing a single classpath entry.
	 */
	private interface Entry {
		/**
		 * Open an input stream to read a resource in the codebase
		 * described by this classpath entry.
		 * 
		 * @param resourceName name of resource to load: e.g., "java/lang/Object.class"
		 * @return an InputStream, or null if the resource wasn't found
		 * @throws IOException if an I/O error occurs
		 */
		public InputStream openStream(String resourceName) throws IOException;
		
		/**
		 * Get filename or URL as string.
		 */
		public String getURL();
	}

	/**
	 * Classpath entry class to load files from a zip/jar file
	 * in the local filesystem.
	 */
	private static class LocalArchiveEntry implements Entry {
		private ZipFile zipFile;
		
		public LocalArchiveEntry(String fileName) throws IOException {
			try {
				zipFile = new ZipFile(fileName);
			} catch (IOException e) {
				IOException ioe = new IOException("Could not open archive file " + fileName);
				ioe.initCause(e);
				throw ioe;
			}
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#openStream(java.lang.String)
		 */
		public InputStream openStream(String resourceName) throws IOException {
			ZipEntry zipEntry = zipFile.getEntry(resourceName);
			if (zipEntry == null)
				return null;
			return zipFile.getInputStream(zipEntry);
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#getURL()
		 */
		public String getURL() {
			return zipFile.getName();
		}
	}
	
	/**
	 * Classpath entry class to load files from a directory
	 * in the local filesystem.
	 */
	private static class LocalDirectoryEntry implements Entry {
		private String dirName;
		
		/**
		 * Constructor.
		 * 
		 * @param dirName name of the local directory
		 * @throws IOException if dirName is not a directory
		 */
		public LocalDirectoryEntry(String dirName) throws IOException {
			this.dirName = dirName;
			if (!(new File(dirName).isDirectory()))
				throw new IOException(dirName + " is not a directory");
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#openStream(java.lang.String)
		 */
		public InputStream openStream(String resourceName) throws IOException {
			File file = new File(dirName, resourceName);
			if (!file.exists())
				return null;
			return new BufferedInputStream(new FileInputStream(file));
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#getURL()
		 */
		public String getURL() {
			return dirName;
		}
		
	}
	
	/**
	 * Classpath entry class to load files from a remote archive URL.
	 * It uses jar URLs to specify individual files within the
	 * remote archive.
	 */
	private static class RemoteArchiveEntry implements Entry {
		private URL remoteArchiveURL;
		
		/**
		 * Constructor.
		 * @param remoteArchiveURL the remote zip/jar file URL
		 */
		public RemoteArchiveEntry(URL remoteArchiveURL) {
			this.remoteArchiveURL = remoteArchiveURL;
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#openStream(java.lang.String)
		 */
		public InputStream openStream(String resourceName) throws IOException {
			URL remoteFileURL = new URL("jar:" + remoteArchiveURL.toString() +
					"/" + resourceName);
			try {
				return remoteFileURL.openStream();
			} catch (IOException e) {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#getURL()
		 */
		public String getURL() {
			return remoteArchiveURL.toString();
		}
		
	}
	
	/**
	 * Classpath entry class to load files from a remote directory URL.
	 */
	private static class RemoteDirectoryEntry implements Entry {
		private URL remoteDirURL;
		
		/**
		 * Constructor.
		 * @param remoteDirURL URL of the remote directory; must end in "/"
		 */
		public RemoteDirectoryEntry(URL remoteDirURL) {
			this.remoteDirURL = remoteDirURL;
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#openStream(java.lang.String)
		 */
		public InputStream openStream(String resourceName) throws IOException {
			URL remoteFileURL = new URL(remoteDirURL.toString() + resourceName);
			try {
				return remoteFileURL.openStream();
			} catch (IOException e) {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.URLClassPath.Entry#getURL()
		 */
		public String getURL() {
			return remoteDirURL.toString();
		}
	}

	// Fields
	private List<Entry> entryList;

	/**
	 * Constructor.
	 * Creates a classpath with no elements.
	 */
	public URLClassPath() {
		this.entryList = new LinkedList<Entry>();
	}
	
	/**
	 * Add given filename/URL to the classpath.
	 * If no URL protocol is given, the filename is assumed
	 * to be a local file or directory.
	 * Remote directories must be specified with a "/" character at the
	 * end of the URL.
	 * 
	 * @param fileName filename or URL of codebase (directory or archive file)
	 * @throws IOException if entry is invalid or does not exist
	 */
	public void addURL(String fileName) throws IOException {
		String protocol = FindBugs.getURLProtocol(fileName);
		if (protocol == null) {
			fileName = "file:" + fileName;
			protocol = "file";
		}
		
		String fileExtension = FindBugs.getFileExtension(fileName);
		boolean isArchive = fileExtension != null && FindBugs.isArchiveExtension(fileExtension);
		
		Entry entry;
		if (protocol.equals("file")) {
			String localFileName = fileName.substring("file:".length());
			
			if (fileName.endsWith("/") || new File(localFileName).isDirectory())
				entry = new LocalDirectoryEntry(localFileName);
			else if (isArchive)
				entry = new LocalArchiveEntry(localFileName);
			else
				throw new IOException("Classpath entry " + fileName +
						" is not a directory or archive file");
		} else {
			if (fileName.endsWith("/"))
				entry = new RemoteDirectoryEntry(new URL(fileName));
			else if (isArchive)
				entry = new RemoteArchiveEntry(new URL(fileName));
			else
				throw new IOException("Classpath entry " + fileName +
						"  is not a remote directory or archive file");
		}
		
		entryList.add(entry);
	}
	
	/**
	 * Return the classpath string.
	 * @return the classpath string
	 */
	public String getClassPath() {
		StringBuffer buf = new StringBuffer();
		for (Iterator<Entry> i = entryList.iterator(); i.hasNext(); ) {
			Entry entry = i.next();
			if (buf.length() > 0)
				buf.append(File.pathSeparator);
			buf.append(entry.getURL());
		}
		return buf.toString();
	}
	
	/**
	 * Open a stream to read given resource.
	 * 
	 * @param resourceName name of resource to load, e.g. "java/lang/Object.class"
	 * @return input stream to read resource, or null if resource
	 *   could not be found
	 * @throws IOException if an IO error occurs trying to determine
	 *   whether or not the resource exists 
	 */
	public InputStream getInputStreamForResource(String resourceName) throws IOException {
		// Try each classpath entry, in order, until we find one
		// that has the resource.  Catch and ignore IOExceptions.
		
		// FIXME: The following code should throw IOException.
		//
		// URL.openStream() does not seem to distinguish
		// whether the resource does not exist, vs. some
		// transient error occurring while trying to access it.
		// This is unfortunate, because we really should throw
		// an exception out of this method in the latter case,
		// since it means our knowledge of the classpath is
		// incomplete.
		//
		// Short of reimplementing HTTP, etc., ourselves,
		// there is probably nothing we can do about this problem.
		
		Iterator<Entry> i = entryList.iterator();
		while (i.hasNext()) {
			Entry entry = i.next();
			InputStream in;
			try {
				in = entry.openStream(resourceName);
				if (in != null)
					return in;
			} catch (IOException ignore) {
				// Ignore
			}
		}
		return null;
	}
	
	/**
	 * Look up a class from the classpath.
	 * 
	 * @param className name of class to look up
	 * @return the JavaClass object for the class
	 * @throws ClassNotFoundException if the class couldn't be found
	 * @throws ClassFormatException if the classfile format is invalid
	 */
	public JavaClass lookupClass(String className) throws ClassNotFoundException {
		String resourceName = className.replace('.', '/') + ".class";
		InputStream in = null;
		boolean parsedClass = false;
		
		try {
			in = getInputStreamForResource(resourceName);
			if (in == null)
				throw new ClassNotFoundException("Error while looking for class " + 
						className + ": class not found");
			
			ClassParser classParser = new ClassParser(in, resourceName);
			JavaClass javaClass = classParser.parse();
			parsedClass = true;
			
			return javaClass;
		} catch (IOException e) {
			throw new ClassNotFoundException("IOException while looking for class " +
					className + ": " + e.toString());
		} finally {
			if (in != null && !parsedClass) {
				try {
					in.close();
				} catch (IOException ignore) {
					// Ignore
				}
			}
		}
	}
}

// vim:ts=4
