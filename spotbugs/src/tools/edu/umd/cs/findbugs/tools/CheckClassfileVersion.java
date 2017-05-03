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

package edu.umd.cs.findbugs.tools;

import java.io.DataInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Verify that a set of jar files are compiled for Java 5.0, the release
 * standard for FindBugs
 *
 */
public class CheckClassfileVersion {

    private static boolean isJarFile(File f) {
        String name = f.getName();
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith("war") || name.endsWith(".ear");
    }

    public static void main(String args[]) throws Exception {

        boolean fail = false;
        ArrayList<File> s = new ArrayList<File>(args.length);
        for (String f : args) {
            File file = new File(f);
            if (!file.canRead()) {
                System.out.println("Can't read " + f);
            }
            if (file.isDirectory()) {
                for (File f2 : file.listFiles()) {
                    if (isJarFile(f2)) {
                        s.add(f2);
                    }
                }
            } else if (isJarFile(file)) {
                s.add(file);
            }
        }

        for (File jarFile : s) {
            String jarFileName = jarFile.getName();
            System.out.println("Checking " + jarFileName);
            try(JarFile z = new JarFile(jarFile)){
                for (Enumeration<JarEntry> e = z.entries(); e.hasMoreElements();) {
                    JarEntry ze = e.nextElement();
                    if (ze.isDirectory()) {
                        continue;
                    }

                    String name = ze.getName();
                    boolean isClassFile = name.endsWith(".class");
                    if (!isClassFile) {
                        continue;
                    }

                    DataInputStream zipIn = new DataInputStream(z.getInputStream(ze));
                    int magic = zipIn.readInt();
                    int minorVersion = zipIn.readUnsignedShort();
                    //                    int majorVersion = zipIn.readUnsignedShort();
                    if (magic != 0xCAFEBABE) {
                        System.out.printf("bad magic %x: %s %s%n", magic, jarFileName, name);
                        fail = true;
                    } else if (minorVersion >= 60) {
                        System.out.printf("bad version %d:%s %s%n", minorVersion, jarFileName, name);
                        fail = true;
                    }
                    zipIn.close();
                }
            }
        }
        if (fail) {
            System.exit(1);
        }
    }

}
