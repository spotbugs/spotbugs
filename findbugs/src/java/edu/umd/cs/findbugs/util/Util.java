/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author William Pugh
 */
public class Util {
	public static final boolean LOGGING = SystemProperties.getBoolean("findbugs.shutdownLogging");

	
	public static Iterable<Integer> setBitIteratable(final BitSet b) {
		return new Iterable<Integer>(){
			public Iterator<Integer> iterator() {
	           return setBitIterator(b);
            }};
	}
		
	public static Iterator<Integer> setBitIterator(final BitSet b) {
		return new Iterator<Integer>() {
			int nextBit =   b.nextSetBit(0);
			
			public boolean hasNext() {
	            return nextBit >= 0;
            }

			public Integer next() {
	           int result = nextBit;
	           nextBit = b.nextSetBit(nextBit+1);
	           return result;
            }

			public void remove() {
	            throw new UnsupportedOperationException(); 
            }};
	}
	public static String repeat(String s, int number) {
		StringBuilder b = new StringBuilder(s.length() * number);
		for(int i = 0; i < number; i++)
			b.append(s);
		return b.toString();
	}
	static Collection<Runnable> runAtShutdown;
	
	public static synchronized void runLogAtShutdown(Runnable r) {
		if (LOGGING) {
			if (runAtShutdown == null) {
				runAtShutdown = new LinkedList<Runnable>();
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
                    public void run() {
						for(Runnable r : runAtShutdown) {
							try {
								r.run();
							} catch (RuntimeException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
			runAtShutdown.add(r);
		}
		
	}
	
	public static <T>  Set<T> emptyOrNonnullSingleton(T t) {
		if (t == null) return Collections.<T>emptySet();
		return Collections.<T>singleton(t);
	}
	public static <K,V> Map<K,V> immutableMap(Map<K,V> map) {
		if (map.size() == 0)
			return Collections.<K,V>emptyMap();
		return Collections.<K,V>unmodifiableMap(map);
	}
	public static int  nullSafeHashcode(@CheckForNull Object o) {
		if (o == null) return 0;
		return o.hashCode();		
	}
	public static <T> boolean  nullSafeEquals(@CheckForNull T o1, @CheckForNull T o2) {
		if (o1 == o2) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}

	public static Reader getReader(InputStream in) throws UnsupportedEncodingException {
		return new InputStreamReader(in, "UTF-8");
	}
	public static Reader getFileReader(String filename) throws UnsupportedEncodingException, FileNotFoundException {
		return getReader(new FileInputStream(filename));
	}
	public static Reader getFileReader(File filename) throws UnsupportedEncodingException, FileNotFoundException {
		return getReader(new FileInputStream(filename));
	}
	public static Writer getWriter(OutputStream out) throws UnsupportedEncodingException, FileNotFoundException {
		return new OutputStreamWriter(out, "UTF-8");
	}

	public static Writer getFileWriter(String filename) throws UnsupportedEncodingException, FileNotFoundException {
		return  getWriter(new FileOutputStream(filename));
	}
	
	public static void closeSilently(InputStream in) {
		try {
			in.close();
		} catch (IOException e) {
			assert true;
		}
	}
	public static void closeSilently(Reader in) {
		try {
			in.close();
		} catch (IOException e) {
			assert true;
		}
	}
	public static void closeSilently(OutputStream out) {
		try {
			out.close();
		} catch (IOException e) {
			assert true;
		}
	}
	static final Pattern tag = Pattern.compile("^\\s*<(\\w+)");
	public static String getXMLType(InputStream in) throws IOException {
		if (!in.markSupported())
			throw new IllegalArgumentException("Input stream does not support mark");

		in.mark(5000);
		BufferedReader r = null;
		try {
			r = new BufferedReader(Util.getReader(in), 2000);

			String s;
			int count = 0;
			while (count < 4) {
				s = r.readLine();
				if (s == null)
					break;
				Matcher m = tag.matcher(s);
				if (m.find())
					return m.group(1);
			}
			throw new IOException("Didn't find xml tag");
		} finally {
			in.reset();
		}

	}
	public static IOException makeIOException(String msg, Throwable cause) {
		IOException e = new IOException(msg);
		e.initCause(cause);
		return e;
	}

	public static String getFileExtension(File f) {
    	String name = f.getName();
    	int lastDot = name.lastIndexOf('.');
    	if (lastDot == -1) return "";
    	return name.substring(lastDot+1).toLowerCase();
    }
}
