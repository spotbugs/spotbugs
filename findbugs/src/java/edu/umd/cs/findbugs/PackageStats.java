/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, Mike Fagan <mfagan@tde.com>
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

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.xml.Dom4JXMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

import java.util.*;

import org.dom4j.Branch;
import org.dom4j.Element;

/**
 * Class to store package bug statistics.
 *
 * @author Mike Fagan
 */
public class PackageStats {
	public static final String ELEMENT_NAME = "PackageStats";
	public static final int ALL_ERRORS = 0;
	private final String packageName;
	// list of errors for this package
	private LinkedList<BugInstance> packageErrors = new LinkedList<BugInstance>();

	// list of all classes and interfaces in package
	private LinkedList<String> packageClasses = new LinkedList<String>();

	// set of interfaces in the package
	private HashSet<String> packageInterfaces = new HashSet<String>();

	public PackageStats(String packageName) {
		this.packageName = packageName;
	}

	public int getTotalPackageTypes() {
		return packageClasses.size();
	}

	public int getTotalPackageErrors() {
		return packageErrors.size();
	}

	public int getNumClasses() throws ClassNotFoundException {
		return countClasses(false);
	}

	public int getNumInnerClasses() {
		int count = 0;
		Iterator<String> itr = packageClasses.iterator();
		while (itr.hasNext()) {
			String name = itr.next();
			if (name.indexOf("$") >= 0) {
				count++;
			}
		}
		return count;
	}

	public int getNumInterfaces() throws ClassNotFoundException {
		return countClasses(true);
	}

	public void addError(BugInstance bug) {
		packageErrors.add(bug);
	}

	public void addClass(String name, boolean isInterface) {
		packageClasses.add(name);
		if (isInterface)
			packageInterfaces.add(name);
	}

	public String getPackageName() {
		return packageName;
	}

	private int countClasses(boolean isInterface) {
		int numInterfaces = packageInterfaces.size();
		if (isInterface)
			return numInterfaces;
		else
			return packageClasses.size() - numInterfaces;
	}

	private List<BugInstance> getErrors(boolean isInterface, int priority) {
		ArrayList<BugInstance> items = new ArrayList<BugInstance>();
		Iterator<BugInstance> itr = packageErrors.iterator();
		while (itr.hasNext()) {
			BugInstance bug = itr.next();
			String clss = bug.getPrimaryClass().getClassName();
			if (packageInterfaces.contains(clss) == isInterface
			        && (priority == bug.getPriority() || priority == ALL_ERRORS)) {
				items.add(bug);
			}
		}
		return items;
	}

	private void toElement(XMLWriteable obj, Branch parent) {
		Dom4JXMLOutput treeBuilder = new Dom4JXMLOutput(parent);
		treeBuilder.write(obj);
	}

	public Element toElement(Branch parent) {
		List<BugInstance> classErrorList = null;
		List<BugInstance> interfaceErrorList = null;
		int classCount = 0;
		int interfaceCount = 0;
		Element element = parent.addElement(ELEMENT_NAME);

		try {
			classErrorList = getErrors(false, ALL_ERRORS);
			interfaceErrorList = getErrors(true, ALL_ERRORS);
			classCount = getNumClasses();
			interfaceCount = getNumInterfaces();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return element;
		}
		element.addAttribute("package", packageName);
		element.addAttribute("total_bugs",
		        String.valueOf(classErrorList.size() + interfaceErrorList.size()));
		element.addAttribute("total_types",
		        String.valueOf(getTotalPackageTypes()));

		Element classes = element.addElement("Classes");
		classes.addAttribute("outer", String.valueOf(classCount - getNumInnerClasses()));
		classes.addAttribute("inner", String.valueOf(getNumInnerClasses()));
		classes.addAttribute("total_bugs",
		        String.valueOf(classErrorList.size()));

		if (classErrorList.size() > 0) {
			Element classErrors = classes.addElement("ClassErrors");
			Iterator<BugInstance> itr = classErrorList.iterator();
			while (itr.hasNext()) {
				BugInstance bug = itr.next();
				//bug.toElement(classErrors);
				toElement(bug, classErrors);
			}
		}

		Element interfaces = element.addElement("Interfaces");
		interfaces.addAttribute("count", String.valueOf(interfaceCount));
		interfaces.addAttribute("total_bugs",
		        String.valueOf(interfaceErrorList.size()));
		if (interfaceErrorList.size() > 0) {
			Element interfaceErrors = classes.addElement("InterfaceErrors");
			Iterator<BugInstance> itr = interfaceErrorList.iterator();
			while (itr.hasNext()) {
				BugInstance bug = itr.next();
				//bug.toElement(interfaceErrors);
				toElement(bug, interfaceErrors);
			}
		}
		return element;
	}

}

// vim:ts=4
