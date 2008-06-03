package edu.umd.cs.findbugs.detect;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class ResolveAllReferences extends PreorderVisitor implements Detector {

	private BugReporter bugReporter;

	public ResolveAllReferences(BugReporter bugReporter) {
		this.bugReporter = bugReporter;

	}

	Set<String> defined;

	private void compute() {
		if (defined == null) {
			// System.out.println("Computing");
			defined = new HashSet<String>();
			
			Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
			Collection<XClass> allClasses = subtypes2.getXClassCollection();
			
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			
			for (XClass c : allClasses) {
				try {
					JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, c.getClassDescriptor());
					addAllDefinitions(jclass);
				} catch (MissingClassException e) {
					bugReporter.reportMissingClass(e.getClassDescriptor());
				} catch (CheckedAnalysisException e) {
					bugReporter.logError("Could not find class " + c.getClassDescriptor().toDottedClassName(), e);
				}
			}
			// System.out.println("Done Computing: " + defined.contains("edu.umd.cs.findbugs.ba.IsNullValueAnalysis.UNKNOWN_VALUES_ARE_NSP : Z"));
		}
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);

	}

	public void report() {
	}

	public void addAllDefinitions(JavaClass obj) {
		String className2 = obj.getClassName();

		defined.add(className2);
		for (Method m : obj.getMethods())
			if (!m.isPrivate()) {
				String name = getMemberName(obj, className2, m.getNameIndex(),
						m.getSignatureIndex());
				defined.add(name);
			}
		for (Field f : obj.getFields())
			if (!f.isPrivate()) {
				String name = getMemberName(obj, className2, f.getNameIndex(), f
						.getSignatureIndex());
				defined.add(name);
			}
	}

	private String getClassName(JavaClass c, int classIndex) {
		String name = c.getConstantPool().getConstantString(classIndex,
				CONSTANT_Class);
		return ClassName.extractClassName(name).replace('/','.');
	}

	private String getMemberName(JavaClass c, String className,
			int memberNameIndex, int signatureIndex) {
		return className
				+ "."
				+ ((ConstantUtf8) c.getConstantPool().getConstant(
						memberNameIndex, CONSTANT_Utf8)).getBytes()
						+ " : "
						+ ((ConstantUtf8) c.getConstantPool().getConstant(
						signatureIndex, CONSTANT_Utf8)).getBytes();
	}
	private String getMemberName(String className,
			String memberName, String signature) {
		return className.replace('/','.')
				+ "."
				+ memberName
						+ " : "
						+ signature;
	}
	private boolean find(JavaClass target, String name, String signature) throws ClassNotFoundException {
		if (target == null) return false;
		String ref = getMemberName(target.getClassName(), name,
				signature);
		if (defined.contains(ref)) return true;
		if (find(target.getSuperClass(), name, signature)) return true;
		for(JavaClass i : target.getInterfaces())
			if (find(i, name, signature)) return true;
		return false;
	}
	@Override
		 public void visit(JavaClass obj) {
		compute();
		ConstantPool cp = obj.getConstantPool();
		Constant[] constants = cp.getConstantPool();
		checkConstant: for (int i = 0; i < constants.length; i++) {
			Constant co = constants[i];
			if (co instanceof ConstantDouble || co instanceof ConstantLong)
				i++;
			if (co instanceof ConstantClass) {
				String ref = getClassName(obj, i);
				if ((ref.startsWith("java") || ref.startsWith("org.w3c.dom")) && !defined.contains(ref))
					bugReporter.reportBug(new BugInstance(this, "VR_UNRESOLVABLE_REFERENCE", NORMAL_PRIORITY)
							.addClass(obj).addString(ref));


			} else if (co instanceof ConstantFieldref) {
				// do nothing until we handle static fields defined in interfaces
			} else if (co instanceof ConstantCP) {
				ConstantCP co2 = (ConstantCP) co;
				String className = getClassName(obj, co2.getClassIndex());

				// System.out.println("checking " + ref);
				if (className.equals(obj.getClassName())
						|| !defined.contains(className)) {
					// System.out.println("Skipping check of " + ref);
					continue checkConstant;
				}
				ConstantNameAndType nt = (ConstantNameAndType) cp
				.getConstant(co2.getNameAndTypeIndex());
				String name = ((ConstantUtf8) obj.getConstantPool().getConstant(
						nt.getNameIndex(), CONSTANT_Utf8)).getBytes();
				String signature = ((ConstantUtf8) obj.getConstantPool().getConstant(
						nt.getSignatureIndex(), CONSTANT_Utf8)).getBytes();


				try {
					JavaClass target = Repository.lookupClass(className);
					if (! find(target, name, signature))
						bugReporter.reportBug(new BugInstance(this, "VR_UNRESOLVABLE_REFERENCE", NORMAL_PRIORITY)
							.addClass(obj).addString(getMemberName(target.getClassName(), name,
									signature)));

				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
				}
			}

		}
	}

}
