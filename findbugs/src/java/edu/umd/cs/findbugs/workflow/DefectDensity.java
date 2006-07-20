
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

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.PackageStats.ClassStats;

/**
 * Java main application to compute defect density for a bug collection
 * (stored as an XML collection)
 * 
 * @author William Pugh
 */
public class DefectDensity {
	private static void printRow(Object... values) {
		for (Object s : values) {
			System.out.print(s);
			System.out.print("\t");
		}
		System.out.println();

	}

	public static double density(int bugs, int ncss) {
		if (ncss == 0) return Double.NaN;
		int bugsPer10KNCSS = 10000*bugs/ncss;
		return bugs/10.0;
	}
	public static void main(String args[]) throws Exception {
		Project project = new Project();
		BugCollection origCollection = new SortedBugCollection();
		int argCount = 0;
		if (argCount == args.length)
			origCollection.readXML(System.in, project);
		else
			origCollection.readXML(args[argCount], project);
		ProjectStats stats = origCollection.getProjectStats();
		printRow("kind", "name", "density/KNCSS", "bugs", "NCSS");
		double projectDensity = density(stats.getTotalBugs(),  stats.getCodeSize());
		printRow("project", origCollection.getCurrentAppVersion().getReleaseName(), 
				projectDensity, stats.getTotalBugs(),
				stats.getCodeSize());
		for (PackageStats p : stats.getPackageStats()) if (p.getTotalBugs() > 4) {
			
			
				double packageDensity = density( p.getTotalBugs(),p.size());
				if (Double.isNaN(packageDensity) || packageDensity < projectDensity) continue;
				printRow("package", p.getPackageName(), 
						packageDensity, p.getTotalBugs(), p.size());
			for (ClassStats c : p.getClassStats()) if (c.getTotalBugs() > 4) {
				double density = density( c.getTotalBugs(), c.size());
				if (Double.isNaN(density) || density < packageDensity) continue;
				printRow("class", c.getName(), 
						density, c.getTotalBugs(), c.size());
			}
		}

	}
	

}
