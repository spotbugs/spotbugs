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

package edu.umd.cs.findbugs.visitclass;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * @author pugh
 */
public class PrintClass {

    /**
     * @author pugh
     */
    static final class ZipEntryComparator implements Comparator<ZipEntry>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(ZipEntry e1, ZipEntry e2) {
            String s1 = e1.getName();
            int pos1 = s1.lastIndexOf('/');
            String p1 = "-";
            if (pos1 >= 0) {
                p1 = s1.substring(0, pos1);
            }

            String s2 = e2.getName();
            int pos2 = s2.lastIndexOf('/');
            String p2 = "-";
            if (pos2 >= 0) {
                p2 = s2.substring(0, pos2);
            }
            int r = p1.compareTo(p2);
            if (r != 0) {
                return r;
            }
            return s1.compareTo(s2);
        }
    }

    static boolean code = false, constants = false;

    static boolean superClasses = false;

    public static void main(String argv[]) throws IOException {
        String[] file_name = new String[argv.length];
        int files = 0;
        String zip_file = null;

        /*
         * Parse command line arguments.
         */
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].charAt(0) == '-') { // command line switch
                if ("-constants".equals(argv[i])) {
                    constants = true;
                } else if ("-code".equals(argv[i])) {
                    code = true;
                } else if ("-super".equals(argv[i])) {
                    superClasses = true;
                } else if ("-zip".equals(argv[i])) {
                    zip_file = argv[++i];
                }
            } else if (argv[i].endsWith(".zip") || argv[i].endsWith(".jar")) {
                zip_file = argv[i];
            } else { // add file name to list
                file_name[files++] = argv[i];
            }
        }

        if (!constants) {
            code = true;
        }
        if (files == 0 && zip_file == null) {
            System.err.println("list: No input files specified");
        } else if (zip_file != null) {
            for (int i = 0; i < files; i++) {
                file_name[i] = file_name[i].replace('.', '/');
            }
            try(ZipFile z = new ZipFile(zip_file)){
                TreeSet<ZipEntry> zipEntries = new TreeSet<ZipEntry>(new ZipEntryComparator());
                for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                    zipEntries.add(e.nextElement());
                }

                for (ZipEntry ze : zipEntries) {
                    String name = ze.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    checkMatch: if (files > 0) {
                        for (int i = 0; i < files; i++) {
                            if (name.indexOf(file_name[i]) >= 0) {
                                break checkMatch;
                            }
                        }
                        continue;
                    }
                    printClass(new ClassParser(z.getInputStream(ze), name));

                }
            }
        } else {
            for (int i = 0; i < files; i++) {
                if (file_name[i].endsWith(".class")) {
                    printClass(new ClassParser(file_name[i]));
                }
            }
        }
    }


    private static void printClass(ClassParser parser) throws IOException {
        JavaClass java_class;
        java_class = parser.parse();

        if (superClasses) {
            try {
                while (java_class != null) {
                    System.out.print(java_class.getClassName() + "  ");
                    java_class = java_class.getSuperClass();
                }
            } catch (ClassNotFoundException e) {
                System.out.println(e.getMessage());

            }
            System.out.println();
            return;
        }
        if (constants || code) {
            System.out.println(java_class); // Dump the contents
        }
        if (constants) {
            System.out.println(java_class.getConstantPool());
        }

        if (code) {
            printCode(java_class.getMethods());
        }
    }

    /**
     * Dump the disassembled code of all methods in the class.
     */
    public static void printCode(Method[] methods) {
        for (Method m : methods) {
            System.out.println(m);
            Code code = m.getCode();
            if (code != null) {
                System.out.println(code);
            }

        }
    }
}
