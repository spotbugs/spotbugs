package edu.umd.cs.daveho.ba;

import java.util.*;
import java.io.*;

/**
 * Class to open input streams on source files.
 * It maintains a "source path", which is like a classpath,
 * but for finding source files instead of class files.
 */
public class SourceFinder {

    private interface SourceBase {
	public InputStream openSource(String fileName) throws IOException;
    }

    private static class DirectorySourceBase implements SourceBase {
	private String dirName;
	public DirectorySourceBase(String dirName) {
	    this.dirName = dirName;
	}

	public InputStream openSource(String fileName) throws IOException {
	    return new FileInputStream(dirName + File.separator + fileName);
	}
    }

    // TODO: JarSourceBase?

    private static final int CACHE_SIZE = 50;

    private static class Cache extends LinkedHashMap<String, byte[]> {
	protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
	    return size() >= CACHE_SIZE;
	}
    }


    private ArrayList<SourceBase> sourceBaseList;
    private Cache cache;

    /**
     * Constructor.
     * @param path the source path, in the same format as a classpath
     */
    public SourceFinder(String path) {
	sourceBaseList = new ArrayList<SourceBase>();
	cache = new Cache();

	StringTokenizer tok = new StringTokenizer(path, File.pathSeparator);
	while (tok.hasMoreTokens()) {
	    String element = tok.nextToken();
	    sourceBaseList.add(new DirectorySourceBase(element));
	}
    }

    /**
     * Open an input stream on a source file for a class in given package.
     * @param packageName the name of the package containing the class whose source file is given
     * @param fileName the unqualified name of the source file
     * @throws IOException if a matching source file cannot be found
     */
    public InputStream openSource(String packageName, String fileName) throws IOException {
	// Create a fully qualified source filename using the package name.
	StringBuffer fullName = new StringBuffer();
	if (!packageName.equals("")) {
	    fullName.append(packageName.replace('.', File.separatorChar));
	    fullName.append(File.separatorChar);
	}
	fullName.append(fileName);
	fileName = fullName.toString();

	// Is the file in the cache already?
	byte[] data = cache.get(fileName);
	if (data == null) {
	     // Find this source file, add its data to the cache
	     //if (DEBUG) System.out.println("Trying "  + fileName + "...");

	    // Query each element of the source path to find the requested source file
	    Iterator<SourceBase> i = sourceBaseList.iterator();	
	    while (i.hasNext()) {
		SourceBase sourceBase = i.next();
		try {
		    // Try to read the file from current source base element
		    InputStream in = sourceBase.openSource(fileName);
		    ByteArrayOutputStream out = new ByteArrayOutputStream();
		    byte[] buf = new byte[1024];
		    int n;
		    while ((n = in.read(buf)) >= 0) {
			out.write(buf, 0, n);
		    }
		    in.close();

		    // Put the data for the file in the cache
		    data = out.toByteArray();
		    cache.put(fileName, data);
		} catch (IOException e) {
		    // Couldn't find the source file in this part of the source base,
		    // so just continue the loop
		    //e.printStackTrace();
		}
	    }

	    // Couldn't find the source file.
	    if (data == null)
		throw new FileNotFoundException("Can't find source file " + fileName);
	}

	return new ByteArrayInputStream(data);
    }

}
