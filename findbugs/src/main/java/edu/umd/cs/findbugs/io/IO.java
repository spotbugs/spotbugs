/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

//   Copyright (C) 2003  William Pugh <pugh@cs.umd.edu>
//   http://www.cs.umd.edu/~pugh
//
//   This library is free software; you can redistribute it and/or
//   modify it under the terms of the GNU Lesser General Public
//   License as published by the Free Software Foundation; either
//   version 2.1 of the License, or (at your option) any later version.
//
//   This library is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//   Lesser General Public License for more details.
//

package edu.umd.cs.findbugs.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;

import javax.annotation.CheckForNull;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import edu.umd.cs.findbugs.util.Util;

public class IO {
    static ThreadLocal<byte[]> myByteBuf = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[4096];
        }
    };

    static ThreadLocal<char[]> myCharBuf = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[4096];
        }
    };

    public static byte[] readAll(@WillClose InputStream in) throws IOException {
        try {
            ByteArrayOutputStream byteSink = new ByteArrayOutputStream();
            copy(in, byteSink);
            return byteSink.toByteArray();
        } finally {
            close(in);
        }
    }

    static byte[] copyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0,
                Math.min(original.length, newLength));
        return copy;
    }


    public static byte[] readAll(@WillClose InputStream in, int size) throws IOException {
        try {
            if (size == 0) {
                throw new IllegalArgumentException();
            }
            byte[] result = new byte[size];
            int pos = 0;
            while (true) {
                int sz;
                while ((sz = in.read(result, pos, size - pos)) > 0) {
                    pos += sz;
                }

                if (pos < size) {
                    return copyOf(result, pos);
                }
                int nextByte = in.read();
                if (nextByte == -1) {
                    return result;
                }
                size = size * 2 + 500;
                result = copyOf(result, size);
                result[pos++] = (byte) nextByte;
            }
        } finally {
            close(in);
        }
    }

    public static String readAll(Reader reader) throws IOException {
        BufferedReader r = new BufferedReader(reader);
        StringWriter w = new StringWriter();
        copy(r, w);
        return w.toString();
    }

    public static long copy(@WillNotClose InputStream in, @WillNotClose OutputStream out) throws IOException {
        return copy(in, out, Long.MAX_VALUE);
    }

    public static long copy(Reader in, Writer out) throws IOException {
        return copy(in, out, Long.MAX_VALUE);
    }

    public static long copy(@WillNotClose InputStream in, @WillNotClose OutputStream out, long maxBytes)

            throws IOException {
        long total = 0;

        int sz = 0;

        byte buf[] = myByteBuf.get();

        while (maxBytes > 0 && (sz = in.read(buf, 0, (int) Math.min(maxBytes, buf.length))) > 0) {
            total += sz;
            maxBytes -= sz;
            out.write(buf, 0, sz);
        }

        return total;
    }

    public static long copy(Reader in, Writer out, long maxChars)

            throws IOException {
        long total = 0;

        int sz;

        char buf[] = myCharBuf.get();

        while (maxChars > 0 && (sz = in.read(buf, 0, (int) Math.min(maxChars, buf.length))) > 0) {
            total += sz;
            maxChars -= sz;
            out.write(buf, 0, sz);
        }
        return total;
    }

    /**
     * Close given InputStream, ignoring any resulting exception.
     *
     */
    public static void close(@CheckForNull Closeable c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Close given InputStream, ignoring any resulting exception.
     *
     * @param inputStream
     *            the InputStream to close; may be null (in which case nothing
     *            happens)
     */
    public static void close(@CheckForNull InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Close given OutputStream, ignoring any resulting exception.
     *
     * @param outputStream
     *            the OutputStream to close; may be null (in which case nothing
     *            happens)
     */
    public static void close(@CheckForNull OutputStream outputStream) {
        if (outputStream == null) {
            return;
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Provide a skip fully method. Either skips the requested number of bytes
     * or throws an IOException;
     *
     * @param in
     *            The input stream on which to perform the skip
     * @param bytes
     *            Number of bytes to skip
     * @throws EOFException
     *             if we reach EOF and still need to skip more bytes
     * @throws IOException
     *             if in.skip throws an IOException
     */
    public static void skipFully(InputStream in, long bytes) throws IOException {
        if (bytes < 0) {
            throw new IllegalArgumentException("Can't skip " + bytes + " bytes");
        }
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                throw new EOFException("Reached EOF while trying to skip a total of " + bytes);
            }
            remaining -= skipped;
        }

    }

    public static boolean verifyURL(URL u) {
        if (u == null) {
            return false;
        }

        InputStream i = null;

        try {
            URLConnection uc = u.openConnection();
            i = uc.getInputStream();
            int firstByte = i.read();
            i.close();
            return firstByte >= 0;
        }

        catch (Exception e) {
            return false;
        } finally {
            Util.closeSilently(i);
        }

    }
}
