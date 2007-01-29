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

package edu.umd.cs.findbugs.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;

/**
 * @author pugh
 */
public class SourceSearcher {
	HashSet<String> sourceFound = new HashSet<String>();
	HashSet<String> sourceNotFound = new HashSet<String>();
	SourceFinder sourceFinder = new SourceFinder();

	public SourceSearcher(Project project) {
		sourceFinder.setSourceBaseList(project.getSourceDirList());
	}
	
	public boolean findSource(SourceLineAnnotation srcLine) {
		if (srcLine == null) return false;
        String cName = srcLine.getClassName();
        if (sourceFound.contains(cName)) return true;
        if (sourceNotFound.contains(cName)) return false;
    
        try {
            InputStream in = sourceFinder.openSource(srcLine);
            in.close();
            sourceFound.add(cName);
            return true;
        } catch (IOException e1) {
            sourceNotFound.add(cName);
            return false;
        }
    }

}
