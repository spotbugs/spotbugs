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
    
}
