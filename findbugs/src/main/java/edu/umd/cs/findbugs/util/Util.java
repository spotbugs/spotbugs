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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.charsets.UTF8;

/**
 * @author William Pugh
 */
public class Util {

    public static Thread startDameonThread(Thread t) {
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static Thread runInDameonThread(Runnable r, String name) {
        Thread t = new Thread(r, name);
        return startDameonThread(t);

    }
    public static Thread runInDameonThread(Runnable r) {
        Thread t = new Thread(r);
        return startDameonThread(t);

    }

    public static int sign(int x) {
        if (x < 0) {
            return -1;
        }
        if (x > 0) {
            return 1;
        }
        return 0;
    }
    /**
     * return sign of x - y
     */
    public static int compare(int x, int y) {
        if (x > y) {
            return 1;
        }
        if (x < y) {
            return -1;
        }
        return 0;
    }

    /**
     * return sign of x - y
     */
    public static int compare(long x, long y) {
        if (x > y) {
            return 1;
        }
        if (x < y) {
            return -1;
        }
        return 0;
    }

    public static Iterable<Integer> setBitIteratable(final BitSet b) {
        return new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return setBitIterator(b);
            }
        };
    }

    public static Iterator<Integer> setBitIterator(final BitSet b) {
        return new Iterator<Integer>() {
            int nextBit = b.nextSetBit(0);

            @Override
            public boolean hasNext() {
                return nextBit >= 0;
            }

            @Override
            public Integer next() {
                int result = nextBit;
                nextBit = b.nextSetBit(nextBit + 1);
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static String repeat(String s, int number) {
        StringBuilder b = new StringBuilder(s.length() * number);
        for (int i = 0; i < number; i++) {
            b.append(s);
        }
        return b.toString();
    }

    static Collection<Runnable> runAtShutdown;

    public static String getNetworkErrorMessage(Throwable e) {
        if ("InvalidProtocolBufferException".equals(e.getClass().getSimpleName())) {
            return "Your Internet provider may require you to log in via your web browser.";
        }
        if (e instanceof UnknownHostException) {
            return "You may not be connected to the Internet.";
        } else {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    static class ShutdownLogging {
        public static final boolean LOGGING = SystemProperties.getBoolean("findbugs.shutdownLogging");
    }

    public static synchronized void runLogAtShutdown(Runnable r) {
        if (ShutdownLogging.LOGGING) {
            if (runAtShutdown == null) {
                runAtShutdown = new LinkedList<Runnable>();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        for (Runnable r : runAtShutdown) {
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

    public static <T> Set<T> emptyOrNonnullSingleton(T t) {
        if (t == null) {
            return Collections.<T> emptySet();
        }
        return Collections.<T> singleton(t);
    }

    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        if (map.size() == 0) {
            return Collections.<K, V> emptyMap();
        }
        return Collections.<K, V> unmodifiableMap(map);
    }

    public static int nullSafeHashcode(@CheckForNull Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }

    public static <T> boolean nullSafeEquals(@CheckForNull T o1, @CheckForNull T o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }

    public static <T extends Comparable<? super T>> int nullSafeCompareTo(@CheckForNull T o1, @CheckForNull T o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        return o1.compareTo(o2);
    }

    public static Reader getReader(@WillCloseWhenClosed InputStream in)  {
        return UTF8.reader(in);
    }

    public static Reader getFileReader(String filename) throws  FileNotFoundException {
        return getReader(new FileInputStream(filename));
    }

    public static Reader getFileReader(File filename) throws  FileNotFoundException {
        return getReader(new FileInputStream(filename));
    }

    public static Writer getWriter(@WillCloseWhenClosed OutputStream out) {
        return UTF8.writer(out);
    }

    public static Writer getFileWriter(String filename) throws FileNotFoundException {
        return getWriter(new FileOutputStream(filename));
    }

    public static void closeSilently(@WillClose Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose PreparedStatement c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose ResultSet c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose Reader in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose OutputStream out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose Closeable out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    public static void closeSilently(@WillClose ZipFile zip) {
        try {
            if (zip != null) {
                zip.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    static final Pattern tag = Pattern.compile("^\\s*<(\\w+)");

    @SuppressFBWarnings("OS_OPEN_STREAM")
    public static String getXMLType(@WillNotClose InputStream in) throws IOException {
        if (!in.markSupported()) {
            throw new IllegalArgumentException("Input stream does not support mark");
        }

        in.mark(5000);
        BufferedReader r = null;
        try {
            r = new BufferedReader(Util.getReader(in), 2000);

            String s;
            int count = 0;
            while (count < 4) {
                s = r.readLine();
                if (s == null) {
                    break;
                }
                Matcher m = tag.matcher(s);
                if (m.find()) {
                    return m.group(1);
                }
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
    private static String getFileExtension(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return name.substring(lastDot + 1);
    }

    public static String getFileExtension(File f) {
        return getFileExtension(f.getName().toLowerCase());
    }
    public static String getFileExtensionIgnoringGz(File f) {

        String name = f.getName().toLowerCase();
        if (name.endsWith(".gz")) {
            name = name.substring(0,name.length()-3);
        }
        return getFileExtension(name);
    }

    public static void throwIOException(String msg, Throwable cause) throws IOException {
        IOException e = new IOException(msg);
        e.initCause(cause);
        throw e;
    }

    /**
     * @param i
     *            the Iterable whose first element is to be retrieved
     * @return first element of iterable
     */
    public static <E> E first(Iterable<E> i) {
        Iterator<E> iterator = i.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("iterator has no elements");
        }
        return iterator.next();
    }

    public static String commonPrefix(String s1, String s2) {
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return s1;
        }
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return s1.substring(0, i);
            }
        }
        if (s1.length() == minLength) {
            return s1;
        }
        assert s2.length() == minLength;
        return s2;

    }

    /** Duplication 1.6 functionality of Collections.newSetFromMap */
    public static <E> Set<E> newSetFromMap(Map<E, Boolean> m) {
        return new SetFromMap<E>(m);
    }

    private static class SetFromMap<E> extends AbstractSet<E> {
        final Map<E, Boolean> m;

        final Set<E> s;

        SetFromMap(Map<E, Boolean> map) {
            this.m = map;
            this.s = map.keySet();
        }

        @Override
        public Iterator<E> iterator() {
            return s.iterator();
        }

        @Override
        public void clear() {
            m.clear();
        }

        @Override
        public int size() {
            return m.size();
        }

        @Override
        public boolean isEmpty() {
            return m.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return m.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return m.remove(o) != null;
        }

        @Override
        public boolean add(E e) {
            return m.put(e, Boolean.TRUE) == null;
        }

        @Override
        public Object[] toArray() {
            return s.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return s.toArray(a);
        }

        @Override
        public String toString() {
            return s.toString();
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return s.equals(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return s.containsAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return s.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return s.retainAll(c);
        }

    }

    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public static <K, V> HashMap<K, V> makeSmallHashMap(Map<K, V> m) {
        HashMap<K, V> result = new HashMap<K, V>((int) (m.size() / DEFAULT_LOAD_FACTOR + 2));
        result.putAll(m);
        return result;

    }

    public static <K> HashSet<K> makeSmallHashSet(Collection<K> m) {
        HashSet<K> result = new HashSet<K>((int) (m.size() / DEFAULT_LOAD_FACTOR + 2));
        result.addAll(m);
        return result;

    }

    public static <K> ArrayList<K> makeSmallArrayList(List<K> m) {
        ArrayList<K> result = new ArrayList<K>(m.size() + 2);
        result.addAll(m);
        return result;

    }

    public static <K> Set<K> addTo(Set<K> s, K k) {
        if (s.isEmpty()) {
            return Collections.singleton(k);
        }
        if (s.contains(k)) {
            return s;
        }
        if (s instanceof HashSet) {
            s.add(k);
            return s;
        }
        HashSet<K> result = makeSmallHashSet(s);
        result.add(k);
        return result;

    }

    public static <K> List<K> addTo(List<K> s, K k) {
        if (s.isEmpty()) {
            return Collections.singletonList(k);
        }
        if (!(s instanceof ArrayList)) {
            s = makeSmallArrayList(s);
        }
        s.add(k);
        return s;
    }

    static public @Nonnull MessageDigest getMD5Digest() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Unable to get MD5 digest", e);
        }
    }

    public static boolean isPowerOfTwo(int i) {
        if (i <= 0) {
            return false;
        }
        if ((i | (i - 1)) + 1 == 2 * i) {
            return true;
        }
        return false;
    }

    public static boolean isPowerOfTwo(long i) {
        if (i <= 0) {
            return false;
        }
        if ((i | (i - 1)) + 1 == 2 * i) {
            return true;
        }
        return false;
    }
}
