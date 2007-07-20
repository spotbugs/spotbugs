/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.Collection;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.jsr305.Analysis;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;

public class TestingGround  implements Detector  {

	BugReporter bugReporter;

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visitClassContext(ClassContext classContext) {
		try {
	        ClassInfo xclass = (ClassInfo) classContext.getXClass();
	        Collection<ClassDescriptor> annotations = xclass.getAnnotationDescriptors();
	        for(ClassDescriptor c : annotations) {
	        	System.out.println(xclass.getDottedClassName()  + " : " + c + " -> " + xclass.getAnnotation(c));
	        }
	        for(MethodDescriptor m : xclass.getMethodDescriptorList()) {
	        	System.out.println(m);
	        	if (m instanceof MethodInfo) {
	        		System.out.println("Method info: ");
	        		
	        		Collection<ClassDescriptor> mAnnotations = ((MethodInfo)m).getAnnotationDescriptors();
			        for(ClassDescriptor c : mAnnotations) {
			        	System.out.println(m.getName()  + " : " + c + " -> " + ((MethodInfo)m).getAnnotation(c));
			        }
	        	for(int i = 0; i < 5; i++) {
	        		Collection<ClassDescriptor> pAnnotations = ((MethodInfo)m).getParameterAnnotationDescriptors(i);
			        if (pAnnotations != null) {
			        	Collection<TypeQualifierAnnotation> annotation2 = TypeQualifierApplications.getApplicableApplications((MethodInfo)m, i);
						System.out.println("#" + i + " : " + annotation2);
			        	for(ClassDescriptor c : pAnnotations) {
			        
			        	System.out.println(m.getName()  + "(" + i + ")  : " + c + " -> " + ((MethodInfo)m).getParameterAnnotation(i, c));
			        }}
	        	}
	        	
	        	}
	        }
	        JavaClass jClass = classContext.getJavaClass();
	        for(Method m : jClass.getMethods()) {
	        	Collection<TypeQualifierValue> result = Analysis.getRelevantTypeQualifiers(
	        			/*classContext, m*/BCELUtil.getMethodDescriptor(classContext.getJavaClass(), m));
	        	if (!result.isEmpty())
	        		System.out.println(m.getName() + " " + m.getSignature() + " : " + result);
	        }
	   
	        
        } catch (CheckedAnalysisException e) {
	        AnalysisContext.logError("Error getting xclass for " + classContext.getClass(), e);
        }
	}

    public void report() {
	    // TODO Auto-generated method stub
	    
    }

}
