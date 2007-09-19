/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.bcel.Constants;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author William Pugh
 */
public class ClassParserUsingASM implements ClassParserInterface {

	
	private  static final BitSet RETURN_OPCODE_SET = new BitSet();
	static {
		RETURN_OPCODE_SET.set(Constants.ARETURN);
		RETURN_OPCODE_SET.set(Constants.IRETURN);
		RETURN_OPCODE_SET.set(Constants.LRETURN);
		RETURN_OPCODE_SET.set(Constants.DRETURN);
		RETURN_OPCODE_SET.set(Constants.FRETURN);
		RETURN_OPCODE_SET.set(Constants.RETURN);
	}

	
	private final ClassReader  classReader;
	private @SlashedClassName String slashedClassName;
	private final ClassDescriptor expectedClassDescriptor;
	private final ICodeBaseEntry codeBaseEntry;
	enum State { INITIAL, THIS_LOADED, VARIABLE_LOADED, AFTER_METHOD_CALL };
	
	
	

	public ClassParserUsingASM(ClassReader classReader,
			@CheckForNull ClassDescriptor expectedClassDescriptor,
			ICodeBaseEntry codeBaseEntry) {
		this.classReader = classReader;
		this.expectedClassDescriptor = expectedClassDescriptor;
		this.codeBaseEntry = codeBaseEntry;
	}
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.engine.ClassParserInterface#parse(edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo.Builder)
	 */
	public void parse(final ClassNameAndSuperclassInfo.Builder cBuilder) throws InvalidClassFileFormatException {

		cBuilder.setCodeBaseEntry(codeBaseEntry);

		final TreeSet<ClassDescriptor> calledClassSet = new TreeSet<ClassDescriptor>();

		classReader.accept(new ClassVisitor(){

			boolean isInnerClass = false;
			
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)  {
				ClassParserUsingASM.this.slashedClassName = name;
				cBuilder.setClassfileVersion(version>>>16, version & 0xffff);
				cBuilder.setAccessFlags(access);
				cBuilder.setClassDescriptor(DescriptorFactory.createClassDescriptor(name));
				cBuilder.setInterfaceDescriptorList(DescriptorFactory.createClassDescriptor(interfaces));
				if (superName != null) cBuilder.setSuperclassDescriptor(DescriptorFactory.createClassDescriptor(superName));
				if (cBuilder instanceof ClassInfo.Builder) {
					((ClassInfo.Builder)cBuilder).setSourceSignature(signature);
				}
			}

			public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean isVisible) {
				if (cBuilder instanceof ClassInfo.Builder) {
					AnnotationValue value = new AnnotationValue(desc);
					((ClassInfo.Builder)cBuilder).addAnnotation(desc, value);
					return value.getAnnotationVisitor();	
				}
				return null;
			}

			public void visitAttribute(Attribute arg0) {
				// TODO Auto-generated method stub

			}

			public void visitEnd() {
				// TODO Auto-generated method stub

			}

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				if (name.equals("this$0")) isInnerClass = true;
				if (cBuilder instanceof ClassInfo.Builder) {
					final FieldInfo.Builder fBuilder = new FieldInfo.Builder(slashedClassName, name, desc, access);
					fBuilder.setSourceSignature(signature);
					return new AbstractFieldAnnotationVisitor() {

						public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
							AnnotationValue value = new AnnotationValue(desc);
							fBuilder.addAnnotation(desc, value);
							return value.getAnnotationVisitor();
						}

						public void visitEnd() {
							((ClassInfo.Builder) cBuilder).addFieldDescriptor(fBuilder.build());

						}

					};

				}
				return null;
			}

			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				if (name.equals(slashedClassName)  && outerName != null) {
					if (cBuilder instanceof ClassInfo.Builder) {
						ClassDescriptor outerClassDescriptor = DescriptorFactory.createClassDescriptor(outerName);
						((ClassInfo.Builder)cBuilder).setImmediateEnclosingClass(outerClassDescriptor);
					}

				}

			}
			
			public MethodVisitor visitMethod(final int access, final String methodName, final String methodDesc, String signature, String[] exceptions) {
				if (cBuilder instanceof ClassInfo.Builder) {
					final MethodInfo.Builder mBuilder = new MethodInfo.Builder(slashedClassName, methodName, methodDesc, access);
					mBuilder.setSourceSignature(signature);
					mBuilder.setThrownExceptions(exceptions);
					
					return new AbstractMethodVisitor(){

						int variable;
						boolean sawReturn = (access & Opcodes.ACC_NATIVE) != 0;
						boolean sawThrow = false;
						State state = State.INITIAL;
						
						
						@Override
						public void visitInsn(int opcode) {
							if (RETURN_OPCODE_SET.get(opcode)) sawReturn = true;
							else if (opcode == Opcodes.ATHROW) sawThrow = true;
							resetState();
						}
						
						public void resetState() {
							if (state != State.AFTER_METHOD_CALL) state = State.INITIAL;
						}
						@Override
						public void visitSomeInsn() {
							resetState();
						}
						@Override
						public void visitVarInsn(int opcode, int var) {
							if (opcode == Opcodes.ALOAD && var == 0)
								state = State.THIS_LOADED;
							else if (state == State.THIS_LOADED) switch(opcode) {
							case Opcodes.ALOAD:
							case Opcodes.ILOAD:
							case Opcodes.LLOAD:
							case Opcodes.DLOAD:
							case Opcodes.FLOAD:
								state = State.VARIABLE_LOADED;
								variable = var;
							} else visitSomeInsn();
						}
						@Override
						public void visitFieldInsn(int opcode,
			                    String owner,
			                    String name,
			                    String desc) {
							if (false && state == State.VARIABLE_LOADED && methodName.equals("<init>") 
									&& owner.equals(slashedClassName) && name.indexOf('$') >= 0) {
								
						
								System.out.println("Parameter " + (variable-1) + " to new " + slashedClassName + methodDesc +  " is synthetic");
							}
							}
						public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
							AnnotationValue value = new AnnotationValue(desc);
							mBuilder.addAnnotation(desc, value);
							return value.getAnnotationVisitor();
						}

						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String desc) {
							if (opcode == Opcodes.INVOKEINTERFACE) return;
							// System.out.println("Call from " + ClassParserUsingASM.this.slashedClassName + " to " + owner + " : " + desc);
							if (desc.indexOf('[') == -1 && desc.indexOf('L') == -1) return;
							if (ClassParserUsingASM.this.slashedClassName.equals(owner)) return;
							ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(owner);
							calledClassSet.add(classDescriptor);
							// System.out.println("Added call from " + ClassParserUsingASM.this.slashedClassName + " to " + owner);
							state = State.AFTER_METHOD_CALL;
						}

						public void visitEnd() {
							if (sawThrow && !sawReturn) mBuilder.setIsUnconditionalThrower();
							MethodInfo methodInfo = mBuilder.build();
							((ClassInfo.Builder)cBuilder).addMethodDescriptor(
									methodInfo);
						}

						public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, String desc,
								boolean visible) {
							if (false)
								for(Iterator<String> i = new SignatureParser(methodDesc).parameterSignatureIterator(); i.hasNext(); ) 
								System.out.println("   " + i.next());
							AnnotationValue value = new AnnotationValue(desc);
							if (isInnerClass && methodName.equals("<init>")) {
								parameter++;
							}
							// System.out.println(isInnerClass + " parameter " + parameter + " of " + slashedClassName+"." + methodName +methodDesc + " is annotated " + desc);
							mBuilder.addParameterAnnotation(parameter, desc, value);
							return value.getAnnotationVisitor();
						}};

				}
				return null;
			}

			public void visitOuterClass(String owner, String name, String desc) {
				
			}

			public void visitSource(String arg0, String arg1) {
				if (cBuilder instanceof ClassInfo.Builder) {
					((ClassInfo.Builder)cBuilder).setSource(arg0);
				}

			}}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		HashSet<ClassDescriptor> referencedClassSet = new HashSet<ClassDescriptor>();

		// collect class references

		int constantPoolCount = classReader.readUnsignedShort(8);
		int offset = 10;
		char [] buf = new char[1024];
		// System.out.println("constant pool count: " + constantPoolCount);
		for(int count = 1; count < constantPoolCount; count++) {
			int tag = classReader.readByte(offset);

			int size;
			switch (tag) {
			case Constants.CONSTANT_Methodref:
			case Constants.CONSTANT_InterfaceMethodref:   
			case Constants.CONSTANT_Fieldref:
			case Constants.CONSTANT_Integer:
			case Constants.CONSTANT_Float:
			case Constants.CONSTANT_NameAndType:
				size = 5;
				break;
			case Constants.CONSTANT_Long:
			case Constants.CONSTANT_Double:
				size = 9;
				count++;
				break;
			case Constants.CONSTANT_Utf8:
				size = 3 + classReader.readUnsignedShort(offset+1);
				break;
			case Constants.CONSTANT_Class:
				@SlashedClassName String className = classReader.readUTF8(offset+1, buf);
				if (className.indexOf('[') >= 0) {
					ClassParser.extractReferencedClassesFromSignature(referencedClassSet, className);
				} else if (ClassName.isValidClassName(className)) {
					ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(className);
					referencedClassSet.add(classDescriptor);
				}
				size = 3;
				break;
				// case ClassWriter.CLASS:
				// case ClassWriter.STR:
			case Constants.CONSTANT_String:
				size = 3;
				break;
			default:
				throw new IllegalStateException("Unexpected tag of " + tag + " at offset " + offset + " while parsing " + slashedClassName + " from " + codeBaseEntry);
			}
			// System.out.println(count + "@" + offset + " : [" + tag +"] size="+size);
			offset += size;
		}
		cBuilder.setCalledClassDescriptors(calledClassSet);
		cBuilder.setReferencedClassDescriptors(referencedClassSet);
	}

	public void parse(ClassInfo.Builder builder) throws InvalidClassFileFormatException {
		parse((ClassNameAndSuperclassInfo.Builder) builder);

	}
}
