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

package edu.umd.cs.findbugs.ba;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.util.Archive;

/**
 * A work-alike class to use instead of BCEL's ClassPath class.
 * The main difference is that URLClassPath can load
 * classfiles from URLs.
 * 
 * @author David Hovemeyer
 */
public class URLClassPath implements Serializable {
	private static final long serialVersionUID = 1L;

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

		/**
		 * Close the underlying resource.
		 */
		public void close();
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

		public void close() {
			try {
				zipFile.close();
			} catch (IOException e) {
				// Ignore
			}
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

		public void close() {
			// Nothing to do here
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

		public void close() {
			// Nothing to do
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

		public void close() {
			// Nothing to do
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
		String protocol = URLClassPath.getURLProtocol(fileName);
		if (protocol == null) {
			fileName = "file:" + fileName;
			protocol = "file";
		}

		String fileExtension = URLClassPath.getFileExtension(fileName);
		boolean isArchive = fileExtension != null && URLClassPath.isArchiveExtension(fileExtension);

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
		for (Entry entry : entryList) {
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
	private InputStream getInputStreamForResource(String resourceName) throws IOException {
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

		for (Entry entry : entryList) {
			InputStream in;
			try {
				in = entry.openStream(resourceName);
				if (in != null) {
					if (URLClassPathRepository.DEBUG) {
						System.out.println("\t==> found " + resourceName + " in " + entry.getURL());
					}
					return in;
				}
			} catch (IOException ignore) {
				// Ignore
			}
		}
		if (URLClassPathRepository.DEBUG) {
			System.out.println("\t==> could not find " + resourceName + " on classpath");
		}
		return null;
	}
	private Set<String> classesThatCantBeFound = new HashSet<String>();
	/**
	 * Look up a class from the classpath.
	 * 
	 * @param className name of class to look up
	 * @return the JavaClass object for the class
	 * @throws ClassNotFoundException if the class couldn't be found
	 */
	public JavaClass lookupClass(String className) throws ClassNotFoundException {
		if (classesThatCantBeFound.contains(className)) {
			throw new ClassNotFoundException("Error while looking for class " + 
					className + ": class not found");
		}
		String resourceName = className.replace('.', '/') + ".class";
		InputStream in = null;
		boolean parsedClass = false;

		try {

			in = getInputStreamForResource(resourceName);
			if (in == null) {
				classesThatCantBeFound.add(className);
				throw new ClassNotFoundException("Error while looking for class " + 
						className + ": class not found");
			}

			ClassParser classParser = new ClassParser(in, resourceName);
			JavaClass javaClass = classParser.parse();
			parsedClass = true;

			return javaClass;
		} catch (IOException e) {
			classesThatCantBeFound.add(className);
			throw new ClassNotFoundException("IOException while looking for class " +
					className ,  e);
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

	/**
	 * Close all underlying resources.
	 */
	public void close() {
		for (Entry entry : entryList) {
			entry.close();
		}
		entryList.clear();
	}

	/**
	 * Get the URL protocol of given URL string.
	 * @param urlString the URL string
	 * @return the protocol name ("http", "file", etc.), or null if there is no protocol
	 */
	public static String getURLProtocol(String urlString) {
		String protocol = null;
		int firstColon = urlString.indexOf(':');
		if (firstColon >= 0) {
			String specifiedProtocol = urlString.substring(0, firstColon);
			if (FindBugs.knownURLProtocolSet.contains(specifiedProtocol))
				protocol = specifiedProtocol;
		}
		return protocol;
	}

	/**
	 * Get the file extension of given fileName.
	 * @return the file extension, or null if there is no file extension
	 */
	public static String getFileExtension(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		return (lastDot >= 0)
			? fileName.substring(lastDot)
			: null;
	}

	/**
	 * Determine if given file extension indicates an archive file.
	 * 
	 * @param fileExtension the file extension (e.g., ".jar")
	 * @return true if the file extension indicates an archive,
	 *   false otherwise
	 */
	public static boolean isArchiveExtension(String fileExtension) {
		return Archive.ARCHIVE_EXTENSION_SET.contains(fileExtension);
	}
}

// vim:ts=4
