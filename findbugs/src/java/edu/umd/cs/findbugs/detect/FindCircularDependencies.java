
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
			if (refClsName.startsWith("java"))
				return;
			
			if (clsName.equals(refClsName))
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
				Iterator it = dependencyGraph.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry entry = (Map.Entry)it.next();
					String clsName = (String)entry.getKey();
					Set<String> dependencies = (Set<String>)entry.getValue();
					
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
			Iterator it = dependencyGraph.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry)it.next();
				String clsName = (String)entry.getKey();
				Set<String> dependencies = (Set<String>)entry.getValue();
				Iterator dit = dependencies.iterator();
				while (dit.hasNext()) {
					if (!dependencyGraph.containsKey(dit.next()))
						dit.remove();
				}
			}
		}
		
		Set<String> alreadyReported = new HashSet<String>();
		Iterator it = dependencyGraph.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			String clsName = (String)entry.getKey();
			if (alreadyReported.contains(clsName))
				continue;
			
			alreadyReported.add(clsName);
			Set<String> dependencies = (Set<String>)entry.getValue();
			
			BugInstance bug = new BugInstance(this, "CD_CIRCULAR_DEPENDENCY", clsName.contains("$") ? LOW_PRIORITY : NORMAL_PRIORITY)
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
