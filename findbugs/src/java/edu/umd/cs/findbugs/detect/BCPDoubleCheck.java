package edu.umd.cs.findbugs.detect;

import java.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.daveho.ba.*;
import edu.umd.cs.daveho.ba.bcp.*;

public class BCPDoubleCheck implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("bcpdc.debug");

	private BugReporter bugReporter;

	private static final int MAX_WILD = 8;
	private static final ByteCodePattern pattern = new ByteCodePattern();
	static {
		pattern
			.setInterElementWild(MAX_WILD)
			.add(new Load("h", "x"))
			.add(new IfNull("x"))
			.add(new Monitorenter(pattern.dummyVariable()))
			.add(new Load("h", "y"))
			.add(new IfNull("y"))
			//.add(new New("z"))
			.add(new Store("h", pattern.dummyVariable()));
	}

	public BCPDoubleCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		// FIXME: prescreen for the existence of
		// MONITORENTER, GETFIELD/GETSTATIC, and PUTFIELD/PUTSTATIC
		// to avoid scanning a lot of methods

		try {
			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative())
					continue;
	
				MethodGen methodGen = classContext.getMethodGen(method);
				ConstantPoolGen cpg = methodGen.getConstantPool();
				CFG cfg = classContext.getCFG(method);
				DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
				ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);

				if (DEBUG) {
					System.out.print(
						"=====================================================================\n"+
						"Method " + methodGen.getName() + "\n" +
						"=====================================================================\n");
				}

				PatternMatcher matcher = new PatternMatcher(pattern, cfg, cpg, dfs, vnaDataflow);
				matcher.execute();
	
				Iterator<ByteCodePatternMatch> j = matcher.byteCodePatternMatchIterator();
				while (j.hasNext()) {
					ByteCodePatternMatch match = j.next();
					BindingSet bindingSet = match.getBindingSet();
	
					Binding binding = bindingSet.lookup("h");
					if (binding == null) {
						bugReporter.logError("Unknown field for BCPDoubleCheck in method " + methodGen);
						continue;
					}
					FieldVariable field = (FieldVariable) binding.getVariable();
					if (field == null) throw new IllegalStateException("Hosed!");

					if (DEBUG) {
						System.out.println("Pattern match:");
						Iterator<PatternElementMatch> pemIter = match.patternElementMatchIterator();
						while (pemIter.hasNext()) {
							PatternElementMatch pem = pemIter.next();
							System.out.println("\t" + pem.toString());
						}
					}
	
					bugReporter.reportBug(new BugInstance("BCPDC_DOUBLECHECK", NORMAL_PRIORITY)
						.addClass(methodGen.getClassName())
						.addMethod(methodGen)
						.addField(field.getClassName(), field.getFieldName(), field.getFieldSig(), field.isStatic()));
				}
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("BCPDoubleCheck caught exception", e);
		}
	}

	public void report() {
	}
}

// vim:ts=4
