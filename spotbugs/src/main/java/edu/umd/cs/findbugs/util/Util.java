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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

    public static String repeat(String s, int number) {
        StringBuilder b = new StringBuilder(s.length() * number);
        for (int i = 0; i < number; i++) {
            b.append(s);
        }
        return b.toString();
    }

    static Collection<Runnable> runAtShutdown;

    static class ShutdownLogging {
        public static final boolean LOGGING = SystemProperties.getBoolean("findbugs.shutdownLogging");
    }

    public static synchronized void runLogAtShutdown(Runnable r) {
        if (ShutdownLogging.LOGGING) {
            if (runAtShutdown == null) {
                runAtShutdown = new LinkedList<>();
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
            return Collections.<T>emptySet();
        }
        return Collections.<T>singleton(t);
    }

    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        if (map.size() == 0) {
            return Collections.<K, V>emptyMap();
        }
        return Collections.<K, V>unmodifiableMap(map);
    }

    public static Reader getReader(@WillCloseWhenClosed InputStream in) {
        return UTF8.reader(in);
    }

    public static Reader getFileReader(String filename) throws FileNotFoundException {
        return getReader(new FileInputStream(filename));
    }

    public static Reader getFileReader(File filename) throws FileNotFoundException {
        return getReader(new FileInputStream(filename));
    }

    public static Writer getWriter(@WillCloseWhenClosed OutputStream out) {
        return UTF8.writer(out);
    }

    public static Writer getFileWriter(String filename) throws FileNotFoundException {
        return getWriter(new FileOutputStream(filename));
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

    /**
     * @deprecated Use try-with-resources instead.
     */
    @Deprecated
    public static void closeSilently(@WillClose Reader in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    /**
     * @deprecated Use try-with-resources instead. And basically {@link IOException} from {@link OutputStream#close()}
     *             is not good to ignore.
     */
    @Deprecated
    public static void closeSilently(@WillClose OutputStream out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    /**
     * @deprecated Use try-with-resources instead. And basically {@link IOException} from {@link OutputStream#close()}
     *             is not good to ignore.
     */
    @Deprecated
    public static void closeSilently(@WillClose Closeable out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            assert true;
        }
    }

    /**
     * @deprecated Use try-with-resources instead.
     */
    @Deprecated
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
        try {
            BufferedReader r = new BufferedReader(Util.getReader(in), 2000);

            while (true) {
                String s = r.readLine();
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
            name = name.substring(0, name.length() - 3);
        }
        return getFileExtension(name);
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

    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public static <K, V> HashMap<K, V> makeSmallHashMap(Map<K, V> m) {
        HashMap<K, V> result = new HashMap<>((int) (m.size() / DEFAULT_LOAD_FACTOR + 2));
        result.putAll(m);
        return result;

    }

    public static <K> HashSet<K> makeSmallHashSet(Collection<K> m) {
        HashSet<K> result = new HashSet<>((int) (m.size() / DEFAULT_LOAD_FACTOR + 2));
        result.addAll(m);
        return result;

    }

    public static <K> ArrayList<K> makeSmallArrayList(List<K> m) {
        ArrayList<K> result = new ArrayList<>(m.size() + 2);
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
        return i > 0
                && (i | (i - 1)) + 1 == 2 * i;
    }

}
