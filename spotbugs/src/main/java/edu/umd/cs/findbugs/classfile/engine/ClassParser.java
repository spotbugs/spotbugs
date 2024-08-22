/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Parse a class to extract symbolic information. see <a
 * href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html">
 * http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html
 * </a>
 *
 * @author David Hovemeyer
 */
public class ClassParser implements ClassParserInterface {

    static class Constant {
        int tag;

        Object[] data;

        Constant(int tag, Object[] data) {
            this.tag = tag;
            this.data = data;
        }
    }

    private final DataInputStream in;

    private final ClassDescriptor expectedClassDescriptor;

    private final ICodeBaseEntry codeBaseEntry;

    private Constant[] constantPool;

    //    private ClassDescriptor immediateEnclosingClass;

    /**
     * Constructor.
     *
     * @param in
     *            the DataInputStream to read class data from
     * @param expectedClassDescriptor
     *            ClassDescriptor expected: null if unknown
     * @param codeBaseEntry
     *            codebase entry class is loaded from
     */
    public ClassParser(DataInputStream in, @CheckForNull ClassDescriptor expectedClassDescriptor, ICodeBaseEntry codeBaseEntry) {
        this.in = in;
        this.expectedClassDescriptor = expectedClassDescriptor;
        this.codeBaseEntry = codeBaseEntry;
    }

    @Override
    public void parse(ClassNameAndSuperclassInfo.Builder builder) throws InvalidClassFileFormatException {
        try {
            int magic = in.readInt();
            if (magic != 0xcafebabe) {
                throw new InvalidClassFileFormatException("Classfile header isn't 0xCAFEBABE", expectedClassDescriptor,
                        codeBaseEntry);
            }
            int major_version = in.readUnsignedShort();
            int minor_version = in.readUnsignedShort();
            int constant_pool_count = in.readUnsignedShort();

            constantPool = new Constant[constant_pool_count];
            for (int i = 1; i < constantPool.length; i++) {
                constantPool[i] = readConstant();
                if (constantPool[i].tag == IClassConstants.CONSTANT_Double
                        || constantPool[i].tag == IClassConstants.CONSTANT_Long) {
                    // Double and Long constants take up two constant pool
                    // entries
                    ++i;
                }
            }

            int access_flags = in.readUnsignedShort();

            int this_class = in.readUnsignedShort();
            ClassDescriptor thisClassDescriptor = getClassDescriptor(this_class);

            int super_class = in.readUnsignedShort();
            ClassDescriptor superClassDescriptor = getClassDescriptor(super_class);

            int interfaces_count = in.readUnsignedShort();
            if (interfaces_count < 0) {
                throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
            }
            ClassDescriptor[] interfaceDescriptorList;
            if (interfaces_count == 0) {
                interfaceDescriptorList = ClassDescriptor.EMPTY_ARRAY;
            } else {
                interfaceDescriptorList = new ClassDescriptor[interfaces_count];
                for (int i = 0; i < interfaceDescriptorList.length; i++) {
                    interfaceDescriptorList[i] = getClassDescriptor(in.readUnsignedShort());
                }
            }
            // Extract all references to other classes,
            // both CONSTANT_Class entries and also referenced method
            // signatures.
            Collection<ClassDescriptor> referencedClassDescriptorList = extractReferencedClasses();

            builder.setClassDescriptor(thisClassDescriptor);
            builder.setSuperclassDescriptor(superClassDescriptor);
            builder.setInterfaceDescriptorList(interfaceDescriptorList);
            builder.setCodeBaseEntry(codeBaseEntry);
            builder.setAccessFlags(access_flags);
            builder.setReferencedClassDescriptors(referencedClassDescriptorList);
            builder.setClassfileVersion(major_version, minor_version);
        } catch (IOException e) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry, e);
        }
    }

    @Override
    public void parse(ClassInfo.Builder builder) throws InvalidClassFileFormatException {
        throw new UnsupportedOperationException("Need to use a ClassParserUsingASM to build ClassInfo");
    }

    /**
     * Extract references to other classes.
     *
     * @return array of ClassDescriptors of referenced classes
     * @throws InvalidClassFileFormatException
     */
    private Collection<ClassDescriptor> extractReferencedClasses() throws InvalidClassFileFormatException {
        Set<ClassDescriptor> referencedClassSet = new HashSet<>();
        for (Constant constant : constantPool) {
            if (constant == null) {
                continue;
            }
            if (constant.tag == IClassConstants.CONSTANT_Class) {
                @SlashedClassName
                String className = getUtf8String((Integer) constant.data[0]);
                if (className.indexOf('[') >= 0) {
                    extractReferencedClassesFromSignature(referencedClassSet, className);
                } else if (ClassName.isValidClassName(className)) {
                    referencedClassSet.add(DescriptorFactory.instance().getClassDescriptor(className));
                }
            } else if (constant.tag == IClassConstants.CONSTANT_Methodref || constant.tag == IClassConstants.CONSTANT_Fieldref
                    || constant.tag == IClassConstants.CONSTANT_InterfaceMethodref) {
                // Get the target class name
                String className = getClassName((Integer) constant.data[0]);
                extractReferencedClassesFromSignature(referencedClassSet, className);

                // Parse signature to extract class names
                String signature = getSignatureFromNameAndType((Integer) constant.data[1]);
                extractReferencedClassesFromSignature(referencedClassSet, signature);
            }
        }
        return referencedClassSet;
    }

    public static void extractReferencedClassesFromSignature(Set<ClassDescriptor> referencedClassSet, String signature) {
        while (signature.length() > 0) {
            int start = signature.indexOf('L');
            if (start < 0) {
                break;
            }
            int end = signature.indexOf(';', start);
            if (end < 0) {
                break;
            }
            @SlashedClassName
            String className = signature.substring(start + 1, end);
            if (ClassName.isValidClassName(className)) {
                referencedClassSet.add(DescriptorFactory.instance().getClassDescriptor(className));
            }
            signature = signature.substring(end + 1);
        }
    }

    // 8: UTF-8 string
    // I: int
    // F: float
    // L: long
    // D: double
    // i: 2-byte constant pool index
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4
    private static final String[] CONSTANT_FORMAT_MAP = { null,
        "8", // 1:CONSTANT_Utf8
        null, // 2:
        "I", // 3: CONSTANT_Integer
        "F", // 4: CONSTANT_Float
        "L", // 5: CONSTANT_Long
        "D", // 6: CONSTANT_Double
        "i", // 7: CONSTANT_Class
        "i", // 8: CONSTANT_String
        "ii", // 9: CONSTANT_Fieldref
        "ii", // 10: CONSTANT_Methodref
        "ii", // 11: CONSTANT_InterfaceMethodref
        "ii", // 12: CONSTANT_NameAndType
        null, // 13:
        null, // 14:
        "bi", // 15: CONSTANT_MethodHandle
        "i", // 16: CONSTANT_MethodType
        "ii", // 17: CONSTANT_Dynamic
        "ii", // 18: CONSTANT_InvokeDynamic
        "i", // 19: CONSTANT_Module
        "i", // 20: CONSTANT_Package
    };

    /**
     * Read a constant from the constant pool. Return null for
     *
     * @return a StaticConstant
     * @throws InvalidClassFileFormatException
     * @throws IOException
     */
    private Constant readConstant() throws InvalidClassFileFormatException, IOException {
        int tag = in.readUnsignedByte();
        if (tag < 0 || tag >= CONSTANT_FORMAT_MAP.length) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }
        String format = CONSTANT_FORMAT_MAP[tag];
        if (format == null) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }

        Object[] data = new Object[format.length()];
        for (int i = 0; i < format.length(); i++) {
            char spec = format.charAt(i);
            switch (spec) {
            case '8':
                data[i] = in.readUTF();
                break;
            case 'I':
                data[i] = in.readInt();
                break;
            case 'F':
                data[i] = Float.valueOf(in.readFloat());
                break;
            case 'L':
                data[i] = in.readLong();
                break;
            case 'D':
                data[i] = Double.valueOf(in.readDouble());
                break;
            case 'i':
                data[i] = in.readUnsignedShort();
                break;
            case 'b':
                data[i] = in.readUnsignedByte();
                break;
            default:
                throw new IllegalStateException();
            }
        }

        return new Constant(tag, data);
    }

    /**
     * Get a class name from a CONSTANT_Class. Note that this may be an array
     * (e.g., "[Ljava/lang/String;").
     *
     * @param index
     *            index of the constant
     * @return the class name
     * @throws InvalidClassFileFormatException
     */
    private @SlashedClassName String getClassName(int index) throws InvalidClassFileFormatException {
        if (index == 0) {
            return null;
        }

        checkConstantPoolIndex(index);
        Constant constant = constantPool[index];
        checkConstantTag(constant, IClassConstants.CONSTANT_Class);

        int refIndex = ((Integer) constant.data[0]).intValue();
        String stringValue = getUtf8String(refIndex);

        return stringValue;
    }

    /**
     * Get the ClassDescriptor of a class referenced in the constant pool.
     *
     * @param index
     *            index of the referenced class in the constant pool
     * @return the ClassDescriptor of the referenced class
     * @throws InvalidClassFileFormatException
     */
    private ClassDescriptor getClassDescriptor(int index) throws InvalidClassFileFormatException {
        @SlashedClassName
        String className = getClassName(index);
        return className != null ? DescriptorFactory.instance().getClassDescriptor(className) : null;
    }

    /**
     * Get the UTF-8 string constant at given constant pool index.
     *
     * @param refIndex
     *            the constant pool index
     * @return the String at that index
     * @throws InvalidClassFileFormatException
     */
    private String getUtf8String(int refIndex) throws InvalidClassFileFormatException {
        checkConstantPoolIndex(refIndex);
        Constant refConstant = constantPool[refIndex];
        checkConstantTag(refConstant, IClassConstants.CONSTANT_Utf8);
        return (String) refConstant.data[0];
    }

    /**
     * Check that a constant pool index is valid.
     *
     * @param index
     *            the index to check
     * @throws InvalidClassFileFormatException
     *             if the index is not valid
     */
    private void checkConstantPoolIndex(int index) throws InvalidClassFileFormatException {
        if (index < 0 || index >= constantPool.length || constantPool[index] == null) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }
    }

    /**
     * Check that a constant has the expected tag.
     *
     * @param constant
     *            the constant to check
     * @param expectedTag
     *            the expected constant tag
     * @throws InvalidClassFileFormatException
     *             if the constant's tag does not match the expected tag
     */
    private void checkConstantTag(Constant constant, int expectedTag) throws InvalidClassFileFormatException {
        if (constant.tag != expectedTag) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }
    }

    interface FieldOrMethodDescriptorCreator<E> {
        public E create(String className, String name, String signature, int accessFlags);
    }

    /**
     * Read field_info, return FieldDescriptor.
     *
     * @param thisClassDescriptor
     *            the ClassDescriptor of this class (being parsed)
     * @return the FieldDescriptor
     * @throws IOException
     * @throws InvalidClassFileFormatException
     *
    private FieldDescriptor readField(ClassDescriptor thisClassDescriptor) throws IOException, InvalidClassFileFormatException {
        return readFieldOrMethod(thisClassDescriptor, new FieldOrMethodDescriptorCreator<FieldDescriptor>() {
            @Override
            public FieldDescriptor create(String className, String name, String signature, int accessFlags) {
                return DescriptorFactory.instance().getFieldDescriptor(className, name, signature,
                        (accessFlags & IClassConstants.ACC_STATIC) != 0);
            }
        });
    }*/

    /**
     * Read method_info, read method descriptor.
     *
    private MethodDescriptor readMethod(ClassDescriptor thisClassDescriptor) throws InvalidClassFileFormatException, IOException {
        return readFieldOrMethod(thisClassDescriptor, new FieldOrMethodDescriptorCreator<MethodDescriptor>() {
    
            @Override
            public MethodDescriptor create(String className, String name, String signature, int accessFlags) {
                return DescriptorFactory.instance().getMethodDescriptor(className, name, signature,
                        (accessFlags & IClassConstants.ACC_STATIC) != 0);
            }
        });
    }*/

    /**
     * Read field_info or method_info. They have the same format.
     *
     * @param <E>
     *            descriptor type to return
     * @param thisClassDescriptor
     *            class descriptor of class being parsed
     * @param creator
     *            callback to create the FieldDescriptor or MethodDescriptor
     * @return the parsed descriptor
     * @throws IOException
     * @throws InvalidClassFileFormatException
     *
    private <E> E readFieldOrMethod(ClassDescriptor thisClassDescriptor, FieldOrMethodDescriptorCreator<E> creator)
            throws IOException, InvalidClassFileFormatException {
        int access_flags = in.readUnsignedShort();
        int name_index = in.readUnsignedShort();
        int descriptor_index = in.readUnsignedShort();
        int attributes_count = in.readUnsignedShort();
    
        String name = getUtf8String(name_index);
        String signature = getUtf8String(descriptor_index);
        if (attributes_count < 0) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }
        for (int i = 0; i < attributes_count; i++) {
            readAttribute();
        }
    
        return creator.create(thisClassDescriptor.getClassName(), name, signature, access_flags);
    }*/

    /**
     * Read an attribute.
     *
     * @throws IOException
     * @throws InvalidClassFileFormatException
     *
    private void readAttribute() throws IOException, InvalidClassFileFormatException {
        int attribute_name_index = in.readUnsignedShort();
        String attrName = getUtf8String(attribute_name_index);
    
        int attribute_length = in.readInt();
        if (attribute_length < 0) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }
    
        if (attrName.equals("InnerClasses")) {
            readInnerClassesAttribute(attribute_length);
        } else {
            IO.skipFully(in, attribute_length);
        }
    }*/

    /**
     * Read an InnerClasses attribute.
     *
     * @param attribute_length
     *            length of attribute (excluding first 6 bytes)
     * @throws InvalidClassFileFormatException
     * @throws IOException
     *
    private void readInnerClassesAttribute(int attribute_length) throws InvalidClassFileFormatException, IOException {
        int number_of_classes = in.readUnsignedShort();
        if (attribute_length != number_of_classes * 8) {
            throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
        }
    
        for (int i = 0; i < number_of_classes; i++) {
            int inner_class_info_index = in.readUnsignedShort();
            int outer_class_info_index = in.readUnsignedShort();
            int inner_name_index = in.readUnsignedShort();
            int inner_class_access_flags = in.readUnsignedShort();
    
            //            if (outer_class_info_index != 0) {
            //                // Record which class this class is a member of.
            //                this.immediateEnclosingClass = getClassDescriptor(outer_class_info_index);
            //            }
        }
    }*/

    /**
     * Get the signature from a CONSTANT_NameAndType.
     *
     * @param index
     *            the index of the CONSTANT_NameAndType
     * @return the signature
     * @throws InvalidClassFileFormatException
     */
    private String getSignatureFromNameAndType(int index) throws InvalidClassFileFormatException {
        checkConstantPoolIndex(index);
        Constant constant = constantPool[index];
        checkConstantTag(constant, IClassConstants.CONSTANT_NameAndType);
        return getUtf8String((Integer) constant.data[1]);
    }
}
