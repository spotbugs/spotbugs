/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

/*
 * ProjectCollection.java
 *
 * Created on March 30, 2003, 8:47 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.*;

/**
 * A collection of projects.
 * This object describes the overall state of the GUI.
 *
 * @author David Hovemeyer
 */
public class ProjectCollection {

    private List projectList;
    
    /** Creates a new instance of ProjectCollection */
    public ProjectCollection() {
	projectList = new LinkedList();
    }

    /**
     * Add a project.
     * @param project the project to add
     */
    public void addProject(Project project) {
	projectList.add(project);
    }
    
    /**
     * Get number of projects.
     * @return number of projects
     */
    public int getNumProjects() { return projectList.size(); }
    
    /**
     * Get project whose number is given.
     * @param num project number
     * @return the project
     */
    public Project getProject(int num) {
        return (Project) projectList.get(num);
    }
    
    public String toString() {
        return "Projects";
    }
}
