package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class ResolveAllReferences extends PreorderVisitor implements Detector,
		Constants2 {

	private BugReporter bugReporter;

	public ResolveAllReferences(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	Set<String> defined;
	private void compute() {
		if (defined == null) {
			defined = new HashSet<String>();
		Subtypes subtypes = AnalysisContext.currentAnalysisContext()
				.getSubtypes();
		Set<JavaClass> allClasses;
		allClasses = subtypes.getAllClasses();
		for(JavaClass c : allClasses) 
			addAllDefinitions(c);
		System.out.println("# of all classes = " + allClasses.size());
		}
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);

	}

	public void report() {
	}

	public void addAllDefinitions(JavaClass obj) {
		System.out.println(obj.getClassName());
		defined.add(getClassName(obj, obj.getClassNameIndex()));
		for (Method m : obj.getMethods())
			defined.add(getMemberName(obj, obj.getClassNameIndex(), m
					.getNameIndex(), m.getSignatureIndex()));
		for (Field f : obj.getFields())
			defined.add(getMemberName(obj, obj.getClassNameIndex(), f
					.getNameIndex(), f.getSignatureIndex()));
	}

	private String getClassName(JavaClass c, int nameIndex) {
		String name = c.getConstantPool().getConstantString(nameIndex,
				CONSTANT_Class);
		if (name.charAt(0) != '[')
			return name;
		while (name.charAt(0) == '[')
			name = name.substring(0);
		if (name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';')
			name = name.substring(1, name.length() - 1);
		return name;
	}

	private String getMemberName(JavaClass c, int classNameIndex,
			int memberNameIndex, int signatureIndex) {
		return getClassName(c, classNameIndex)
				+ "."
				+ ((ConstantUtf8)c.getConstantPool().getConstant(memberNameIndex,
						CONSTANT_Utf8)).getBytes()
				+ "."
				+ ((ConstantUtf8)c.getConstantPool().getConstant(signatureIndex,
						CONSTANT_Utf8)).getBytes()
				;
	}

	public void visit(JavaClass obj) {
		compute();
		ConstantPool cp = obj.getConstantPool();
		Constant[] constants = cp.getConstantPool();
		for (int i = 0; i < constants.length; i++) {
			Constant co = constants[i];
			if (co instanceof ConstantDouble || co instanceof ConstantLong)
				i++;
			if (co instanceof ConstantClass) {
				String ref = getClassName(obj, i);
				if (!defined.contains(ref))
					System.out.println(getClassName()
							+ " makes unresolvable reference to " + ref 
			+ " : " 
			+ defined.size());

			} else if (co instanceof ConstantCP) {
				ConstantCP co2 = (ConstantCP) co;
				ConstantNameAndType nt = (ConstantNameAndType) cp
						.getConstant(co2.getNameAndTypeIndex());
				String ref = getMemberName(obj, co2.getClassIndex(), nt
						.getNameIndex(), nt.getSignatureIndex());
				if (!defined.contains(ref))
					System.out.println(getClassName()
							+ " makes unresolvable reference to " + ref
			+ " : " 
			+ defined.size());

			}
		}

	}

}
