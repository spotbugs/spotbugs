/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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
package edu.umd.cs.findbugs.workflow;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.PackageStats.ClassStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.util.Util;

/**
 * For each source file that has reported bugs, compute a hash of all the issues
 * reported for that file. These hashes use line numbers, so a change that only
 * changes the line number of an issue will cause the hash to be different.
 *
 * @author William Pugh
 */
public class FileBugHash {

    Map<String, StringBuilder> hashes = new LinkedHashMap<String, StringBuilder>();

    Map<String, Integer> counts = new HashMap<String, Integer>();

    Map<String, Integer> sizes = new HashMap<String, Integer>();

    MessageDigest digest = Util.getMD5Digest();

    FileBugHash(BugCollection bugs) {

        for (PackageStats pStat : bugs.getProjectStats().getPackageStats()) {
            for (ClassStats cStat : pStat.getSortedClassStats()) {
                String path = cStat.getName();
                if (path.indexOf('.') == -1) {
                    path = cStat.getSourceFile();
                } else {
                    path = path.substring(0, path.lastIndexOf('.') + 1).replace('.', '/') + cStat.getSourceFile();
                }
                counts.put(path, 0);
                Integer size = sizes.get(path);
                if (size == null) {
                    size = 0;
                }
                sizes.put(path, size + cStat.size());
            }
        }
        for (BugInstance bug : bugs.getCollection()) {
            SourceLineAnnotation source = bug.getPrimarySourceLineAnnotation();

            String packagePath = source.getPackageName().replace('.', '/');
            String key;
            if (packagePath.length() == 0) {
                key = source.getSourceFile();
            } else {
                key = packagePath + "/" + source.getSourceFile();
            }
            StringBuilder buf = hashes.get(key);
            if (buf == null) {
                buf = new StringBuilder();
                hashes.put(key, buf);
            }
            buf.append(bug.getInstanceKey()).append("-").append(source.getStartLine()).append(".")
            .append(source.getStartBytecode()).append(" ");
            Integer count = counts.get(key);
            if (count == null) {
                counts.put(key, 1);
            } else {
                counts.put(key, 1 + count);
            }
        }
    }

    public Collection<String> getSourceFiles() {
        return counts.keySet();
    }

    public @CheckForNull
    String getHash(String sourceFile) {
        StringBuilder rawHash = hashes.get(sourceFile);
        if (rawHash == null || digest == null) {
            return null;
        }
        byte[] data = digest.digest(UTF8.getBytes(rawHash.toString()));
        String tmp = new BigInteger(1, data).toString(16);
        if (tmp.length() < 32) {
            tmp = "000000000000000000000000000000000".substring(0, 32 - tmp.length()) + tmp;
        }
        return tmp;
    }

    public int getBugCount(String sourceFile) {
        Integer count = counts.get(sourceFile);
        if (count == null) {
            return 0;
        }
        return count;
    }

    public int getSize(String sourceFile) {
        Integer size = sizes.get(sourceFile);
        if (size == null) {
            return 0;
        }
        return size;
    }

    public static void main(String args[]) throws Exception {

        if (args.length > 1 || (args.length > 0 && "-help".equals(args[0]))) {
            System.err.println("Usage: " + FileBugHash.class.getName() + " [<infile>]");
            System.exit(1);
        }
        BugCollection origCollection = new SortedBugCollection();
        int argCount = 0;
        if (argCount == args.length) {
            origCollection.readXML(System.in);
        } else {
            origCollection.readXML(args[argCount]);
        }
        FileBugHash result = compute(origCollection);
        for (String sourceFile : result.getSourceFiles()) {
            System.out.println(result.getHash(sourceFile) + "\t" + sourceFile);
        }

    }

    public static FileBugHash compute(BugCollection origCollection) {
        return new FileBugHash(origCollection);
    }
}
