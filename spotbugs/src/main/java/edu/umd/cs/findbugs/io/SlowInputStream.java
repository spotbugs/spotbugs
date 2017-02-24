/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SlowInputStream extends FilterInputStream {
    private final long started = System.currentTimeMillis();

    private long length = 0;

    private final int bytesPerSecond;

    public SlowInputStream(InputStream in, int baudRate) {
        super(in);
        this.bytesPerSecond = baudRate / 10;
    }

    private void delay() {
        try {
            long beenRunning = System.currentTimeMillis() - started;
            long time = length * 1000L / bytesPerSecond - beenRunning;
            if (time > 0) {
                Thread.sleep((int) time);
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b >= 0) {
            length++;
        }
        delay();
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len > bytesPerSecond / 10) {
            len = bytesPerSecond / 10;
        }
        int tmp = in.read(b, off, len);
        if (tmp >= 0) {
            length += tmp;
            delay();
        }
        return tmp;
    }

    @Override
    public long skip(long n) throws IOException {
        n = in.skip(n);
        if (n >= 0) {
            length += n;
        }
        delay();
        return n;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
