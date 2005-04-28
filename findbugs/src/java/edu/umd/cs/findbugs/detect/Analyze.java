package edu.umd.cs.findbugs.detect;

import java.util.*;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.TypedInstruction;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.ch.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.TopType;
import edu.umd.cs.findbugs.ba.NullType;
import edu.umd.cs.findbugs.ba.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.ValueNumber;
import edu.umd.cs.findbugs.ba.TypeDataflow;
import edu.umd.cs.findbugs.ba.TypeFrame;

public class Analyze {


	public static double deepInstanceOf(JavaClass x, JavaClass y)  {
		try {
		boolean upcast = Repository.instanceOf( x, y);
		if (upcast) return 1.0;
		boolean downcast = Repository.instanceOf(y, x);
		if (!downcast) {
			if (x.isFinal() || y.isFinal()) return 0.0;
			if (!x.isInterface() && !y.isInterface()) return 0.0;
			}

		String xName = x.getClassName().replace('/','.');
		String yName = y.getClassName().replace('/','.');
		Subtypes subtypes= AnalysisContext.currentAnalysisContext().getSubtypes();

		Set<JavaClass> xSubtypes 
			= subtypes.getTransitiveSubtypes(x);

		Set<JavaClass> ySubtypes 
			= subtypes.getTransitiveSubtypes(y);

		Set<JavaClass> both = new HashSet<JavaClass>(xSubtypes);
		both.retainAll(ySubtypes);
		Set<JavaClass> xButNotY = new HashSet<JavaClass>(xSubtypes);
		xButNotY.removeAll(ySubtypes);

		boolean concreteClassesInBoth = false;
		for(JavaClass v : both)
			if (v.isAbstract())
				concreteClassesInBoth = true;

		boolean concreteClassesInXButNotY = false;
		for(JavaClass v : xButNotY)
			if (v.isAbstract())
				concreteClassesInXButNotY = true;

		if (downcast) {
			if (!concreteClassesInXButNotY) return 1.0;
			return 0.7;
			}
			
		if (both.isEmpty()) {
			if (concreteClassesInXButNotY) {
				return 0.1;
				}
			return 0.2;
			}
		if (!concreteClassesInXButNotY) {
			if (concreteClassesInBoth) return 1.0;
			return 0.7;
			}
		if (concreteClassesInBoth) return 0.5;
		return 0.4;
		} catch (ClassNotFoundException e) {
			return 1.0;
			}
		}
	}
