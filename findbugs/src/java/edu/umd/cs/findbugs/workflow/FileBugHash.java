
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
import java.util.Map;
import java.util.TreeMap;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * For each source file that has reported bugs, compute a hash of all the issues reported for that file.
 * These hashes use line numbers, so a change that only changes the line number of an issue will cause the hash to be different.
 * 
 * @author William Pugh
 */
public class FileBugHash {

	public static void main(String args[]) throws Exception {

		if (args.length>1 || (args.length>0 && "-help".equals(args[0]))) {
			System.err.println("Usage: " + FileBugHash.class.getName() + " [<infile>]");
			System.exit(1);
		}
		MessageDigest digest = MessageDigest.getInstance("MD5");
		Project project = new Project();
		BugCollection origCollection = new SortedBugCollection();
		int argCount = 0;
		if (argCount == args.length)
			origCollection.readXML(System.in, project);
		else
			origCollection.readXML(args[argCount], project);
		Map<String, StringBuilder> map = new TreeMap<String, StringBuilder>();
		
		for(BugInstance bug : origCollection.getCollection()) {
			SourceLineAnnotation source = bug.getPrimarySourceLineAnnotation();
			ClassAnnotation classAnnotation = bug.getPrimaryClass();
			MethodAnnotation methodAnnotation = bug.getPrimaryMethod();
			FieldAnnotation fieldAnnotation = bug.getPrimaryField();

			if (!source.isSourceFileKnown()) {
				System.out.println(bug);
				System.out.println(classAnnotation);
				System.out.println(fieldAnnotation);
				System.out.println(methodAnnotation);
				source = bug.getPrimarySourceLineAnnotation();
			}
			String key = source.getPackageName().replace('.','/')+"/"+source.getSourceFile();
			StringBuilder buf = map.get(key);
			if (buf == null) {
				buf = new StringBuilder();
				map.put(key,buf);
			}
			buf.append(bug.getInstanceKey()).append("-").append(source.getStartLine()).append(".").append(source.getStartBytecode()).append(" ");
		}
		for(Map.Entry<String, StringBuilder> e : map.entrySet()) {
			byte [] data = digest.digest(e.getValue().toString().getBytes());
			String tmp = new BigInteger(1,data).toString(16);
			if (tmp.length() < 32) tmp = "000000000000000000000000000000000".substring(0,32-tmp.length())+tmp;
			System.out.println(tmp + "\t" + e.getKey());
		}
		
	}


}
