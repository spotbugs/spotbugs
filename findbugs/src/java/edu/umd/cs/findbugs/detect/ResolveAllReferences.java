package edu.umd.cs.findbugs.detect;


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

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
			Subtypes subtypes = AnalysisContext.currentAnalysisContext()
					.getSubtypes();
			Set<JavaClass> allClasses;
			allClasses = subtypes.getAllClasses();
			for (JavaClass c : allClasses)
				addAllDefinitions(c);
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
		return Subtypes.extractClassName(name);
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
				String ref = getClassName(obj, i).replace('/','.');
				if (ref.startsWith("java") && !defined.contains(ref))
					bugReporter.reportBug(new BugInstance(this, "VR_UNRESOLVABLE_REFERENCE", NORMAL_PRIORITY)
					        .addClass(obj).addString(ref));
					

			} else if (co instanceof ConstantFieldref) {
				// do nothing until we handle static fields defined in interfaces
			} else if (co instanceof ConstantCP) {
				ConstantCP co2 = (ConstantCP) co;
				String className = getClassName(obj, co2.getClassIndex()).replace('/','.');

				ConstantNameAndType nt = (ConstantNameAndType) cp
						.getConstant(co2.getNameAndTypeIndex());

				String ref = getMemberName(obj, className, nt.getNameIndex(),
						nt.getSignatureIndex());
				// System.out.println("checking " + ref);
				if (className.equals(obj.getClassName())
						|| !defined.contains(className)) {
					// System.out.println("Skipping check of " + ref);
					continue checkConstant;
				}
				
				if (defined.contains(ref)) {
					
					continue checkConstant;
				}
			
				
				try {
					JavaClass target = Repository.lookupClass(className);
					if (target.isInterface() || target.isAbstract()) {
						// System.out.println("Skipping interface for " + ref);
						continue checkConstant;
					}
					while (true) {
						
						target = target.getSuperClass();
						if (target == null || !defined.contains(target.getClassName())) break;
						
						String ref2 = getMemberName(obj, target.getClassName(), nt
								.getNameIndex(), nt.getSignatureIndex());
						if (defined.contains(ref2)) {
							
							continue checkConstant;
						}
					}
					bugReporter.reportBug(new BugInstance(this, "VR_UNRESOLVABLE_REFERENCE", NORMAL_PRIORITY)
					        .addClass(obj).addString(ref));
					
				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
				}
			}

		}
	}

}
