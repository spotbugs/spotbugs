/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.mbeans;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * @author pugh
 */
public class AnalysisBeanImpl implements AnalysisMXBean {
    
    public static AnalysisBeanImpl makeAnalysisBean() {
        // Create the Hello World MBean
        AnalysisBeanImpl mbean = new AnalysisBeanImpl();

    
        try {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Construct the ObjectName for the MBean we will register
        ObjectName name = new ObjectName("edu.umd.cs.findbugs:type=Analysis");

       
        // Register the Hello World MBean
        mbs.registerMBean(mbean, name);
        } catch (Exception e) {
            AnalysisContext.logError("Unable to register analysis bean", e);
        }
        return mbean;
    }
    
    public void deregister() {
        try {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("edu.umd.cs.findbugs:type=Analysis");
        mbs.unregisterMBean(name);
        } catch (Exception e) {
            AnalysisContext.logError("Unable to register analysis bean", e);
        }
    }
    

    int phase;
    int completed;
    int total;
    String analyzing;
    
    int errors;
    /**
     * @return Returns the errors.
     */
    public int getErrors() {
        return errors;
    }

    /**
     * @param errors The errors to set.
     */
    public void setErrors(int errors) {
        this.errors = errors;
    }

    /**
     * @return Returns the phase.
     */
    public int getPhase() {
        return phase;
    }
    /**
     * @param phase The phase to set.
     */
    public void setPhase(int phase) {
        this.phase = phase;
    }
    /**
     * @return Returns the completed.
     */
    public int getCompleted() {
        return completed;
    }
    /**
     * @param completed The completed to set.
     */
    public void setCompleted(int completed) {
        this.completed = completed;
    }
    /**
     * @return Returns the total.
     */
    public int getTotal() {
        return total;
    }
    /**
     * @param total The total to set.
     */
    public void setTotal(int total) {
        this.total = total;
    }
    /**
     * @return Returns the analyzing.
     */
    public String getAnalyzing() {
        return analyzing;
    }
    /**
     * @param analyzing The analyzing to set.
     */
    public void setAnalyzing(String analyzing) {
        this.analyzing = analyzing;
    }
 
   
}
