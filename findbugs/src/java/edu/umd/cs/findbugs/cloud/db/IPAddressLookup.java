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

package edu.umd.cs.findbugs.cloud.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public final class IPAddressLookup {

    volatile String ipAddress = "unknown";

    CountDownLatch latch = new CountDownLatch(1);

    IPAddressLookup() {
        Thread t = new Thread(new Runnable() {

            public void run() {
                BufferedReader in = null;
                try {
                    URL u = new URL("http://www.whatismyip.org/");
                    URLConnection c = u.openConnection();

                    in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    ipAddress = in.readLine();

                } catch (IOException e) {
                    assert true;
                } finally {

                    latch.countDown();
                    Util.closeSilently(in);
                }

            }
        });
        t.setDaemon(true);
        t.start();
    }

    public String get() {
        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assert true;
        }
        return ipAddress;
    }

}
