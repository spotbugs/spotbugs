/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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
package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.visitclass.Constants2;


public class FindCircularDependencies extends BytecodeScanningDetector implements Constants2
{
	private static HashMap<String, Set<String>> dependencyGraph = null;

	private BugReporter bugReporter;
	private String clsName;
	
	public FindCircularDependencies(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void setAnalysisContext(AnalysisContext analysisContext){
		dependencyGraph = new HashMap<String,Set<String>>();
	}
	
	public void visit(JavaClass obj) {
		clsName = obj.getClassName();
	}
	
	public void sawOpcode(int seen) {
		if ((seen == INVOKESPECIAL)
		||  (seen == INVOKESTATIC)
		||  (seen == INVOKEVIRTUAL)) {
			String refClsName = getClassConstantOperand();
			refClsName = refClsName.replace('/', '.');
			if (refClsName.startsWith("java"))
				return;
			
			if (clsName.equals(refClsName))
				return;
				
			if (clsName.startsWith(refClsName) && (refClsName.indexOf("$") >= 0))
				return;
			
			if (refClsName.startsWith(refClsName) && (clsName.indexOf("$") >= 0))
				return;
		
			Set<String> dependencies = dependencyGraph.get(clsName);
			if (dependencies == null) {
				dependencies = new HashSet<String>();
				dependencyGraph.put(clsName, dependencies);
			}
			
			dependencies.add(refClsName);
		}
	}
	
	public void report() {
		{	//Remove classes that don't have cycles
			boolean changed = true;
			while (changed) {
				changed = false;
				Iterator<Map.Entry<String, Set<String>>> it = dependencyGraph.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, Set<String>> entry = it.next();
					String clsName = entry.getKey();
					Set<String> dependencies = entry.getValue();
					
					boolean foundClass = false;
					Iterator dit = dependencies.iterator();
					while (dit.hasNext()) {
						foundClass = dependencyGraph.containsKey(dit.next());
						if (foundClass)
							break;
					}
					if (!foundClass) {
						it.remove();
						changed = true;
					}
				}
			}
		}
		
		{	//Remove references that are not roots
			Iterator<Map.Entry<String, Set<String>>> it = dependencyGraph.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Set<String>> entry = it.next();
				String clsName = entry.getKey();
				Set<String> dependencies = entry.getValue();
				Iterator dit = dependencies.iterator();
				while (dit.hasNext()) {
					if (!dependencyGraph.containsKey(dit.next()))
						dit.remove();
				}
				if (dependencies.size() == 0)
					it.remove();
			}
		}
		
		Set<String> alreadyReported = new HashSet<String>();
		Iterator<Map.Entry<String, Set<String>>> it = dependencyGraph.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Set<String>> entry = it.next();
			String clsName = entry.getKey();
			if (alreadyReported.contains(clsName))
				continue;
			
			alreadyReported.add(clsName);
			Set<String> dependencies = entry.getValue();
			
			BugInstance bug = new BugInstance(
					this,
					"CD_CIRCULAR_DEPENDENCY",
					clsName.indexOf("$") >= 0 ? LOW_PRIORITY : NORMAL_PRIORITY)
					.addClass(clsName);
			
		    Iterator dit = dependencies.iterator();
		    while (dit.hasNext()) {
		    	clsName = (String)dit.next();
		    	bug.addClass(clsName);
		    	alreadyReported.add(clsName);
		    }

		    bugReporter.reportBug(bug);
		}
		dependencyGraph.clear();
	}
}
