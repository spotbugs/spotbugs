package edu.umd.cs.findbugs.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.WillClose;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.Util;

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

/**
 * Filter a property database, only passing through the annotations on public or
 * protected methods
 */
public class FilterAndCombineBitfieldPropertyDatabase {

    final static int FLAGS = Constants.ACC_PROTECTED | Constants.ACC_PUBLIC;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Map<String, Integer> properties = new TreeMap<String, Integer>();
        Map<String, Integer> accessFlags = new TreeMap<String, Integer>();

        if (args.length == 0) {
            process(System.in, properties, accessFlags);
        } else {
            for (String f : args) {
                process(new FileInputStream(f), properties, accessFlags);
            }
        }

        for (Entry<String, Integer> e : properties.entrySet()) {
            String key = e.getKey();
            System.out.println(key + "," + accessFlags.get(key) + "|" + e.getValue());
        }
    }

    enum Status {
        NOT_FOUND, EXPOSED, UNEXPOSED
    }

    static Map<String, Status> classStatus = new HashMap<String, Status>();

    static Status getStatus(@DottedClassName String name) {
        if (name.startsWith("com.sun") || name.startsWith("com.oracle")
                || name.startsWith("sun") || name.startsWith("netscape")) {
            return Status.UNEXPOSED;
        }
        Status result = classStatus.get(name);
        if (result != null) {
            return result;
        }

        try {
            Class<?> c = Class.forName(name, false, ClassLoader.getSystemClassLoader());
            int accessFlags = c.getModifiers();
            if ((accessFlags & FLAGS) != 0) {
                result = Status.EXPOSED;
            } else {
                result = Status.UNEXPOSED;
            }
        } catch (Exception e) {
            result = Status.NOT_FOUND;
            // System.out.println("# can't find " + name);
        }
        classStatus.put(name, result);
        return result;
    }

    /**
     * @param inSource
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private static void process(@WillClose InputStream inSource, Map<String, Integer> properties, Map<String, Integer> accessFlags)
            throws UnsupportedEncodingException, IOException {
        BufferedReader in = new BufferedReader(Util.getReader(inSource));
        Pattern p = Pattern.compile("^(([^,]+),.+),([0-9]+)\\|([0-9]+)$");
        try {
            while (true) {
                String s = in.readLine();
                if (s == null) {
                    break;
                }
                Matcher m = p.matcher(s);
                if (m.find()) {
                    String key = m.group(1);
                    String className = m.group(2);
                    if (getStatus(className) == Status.UNEXPOSED) {
                        continue;
                    }
                    int accFlags = Integer.parseInt(m.group(3));
                    int bits = Integer.parseInt(m.group(4));
                    if ((accFlags & FLAGS) != 0) {
                        accessFlags.put(key, accFlags);
                        if (properties.containsKey(key)) {
                            properties.put(key, bits | properties.get(key));
                        } else {
                            properties.put(key, bits);
                        }
                    }
                }

            }
        } finally {
            Util.closeSilently(in);
        }
    }

}
