package edu.umd.cs.findbugs.visitclass;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

public class AnnotationVisitor extends PreorderVisitor {

    static final boolean DEBUG = false;

    public void visitAnnotation(String annotationClass, Map<String, Object> map, boolean runtimeVisible)  {
	System.out.println("Annotation: " + annotationClass);
	for(Map.Entry<String,Object>  e : map.entrySet()) {
		System.out.println("    " + e.getKey());
		System.out.println(" -> " + e.getValue());
		}
	}
    public void visit(Attribute obj)  {
        try {
            if (obj instanceof Unknown) {
                String name = ((Unknown)obj).getName();
                if (DEBUG) 
		System.out.println("In " + getDottedClassName() + " found " + 
                    name);
                if (!(name.equals("RuntimeVisibleAnnotations") || name.equals("RuntimeInvisibleAnnotations"))) return;
                byte [] b = ((Unknown)obj).getBytes();
                DataInputStream bytes = new DataInputStream(new ByteArrayInputStream(b));
                int numAnnotations = bytes.readUnsignedShort();
                if (DEBUG)
		System.out.println("# of annotations: " + numAnnotations);
                for(int i = 0; i < numAnnotations; i++) {
                    int annotationNameIndex = bytes.readUnsignedShort();
            		String annotationName = 
	((ConstantUtf8)getConstantPool().getConstant(annotationNameIndex)).getBytes();
			annotationName = annotationName.substring(1,annotationName.length()-1);
                if (DEBUG)
                    System.out.println("Annotation name: " + annotationName);
                    int numPairs = bytes.readUnsignedShort();
		    Map<String,Object> values = new HashMap<String,Object>();
                    for(int j = 0; j < numPairs; j++) {
                        int memberNameIndex = bytes.readUnsignedShort();
            		String memberName = 
	((ConstantUtf8)getConstantPool().getConstant(memberNameIndex)).getBytes();
                if (DEBUG)
                        System.out.println("memberName: " + memberName);
                        Object value = readAnnotationValue(bytes);
                if (DEBUG)
			System.out.println(memberName + ":" + value);
			values.put(memberName,value);
                    }
		    visitAnnotation(annotationName, values,
				name.equals("RuntimeVisibleAnnotations"));
                }


                if (DEBUG) {
                for(int i = 0; i < b.length; i++)
                    System.out.print(Integer.toString((b[i] & 0xff), 16) + " ");
                System.out.println();
		}
            }
		
        } 
        catch (Exception e) {
            // ignore
        }
    }
    private Object readAnnotationValue(DataInputStream bytes) throws IOException {
        char tag = (char) bytes.readUnsignedByte();
                if (DEBUG) 
        System.out.println("tag: " + tag);
        switch(tag) {
        case '[':
            int sz = bytes.readUnsignedShort();
                if (DEBUG) 
            System.out.println("Array of " + sz + " entries");
            Object [] result = new Object[sz];
            for(int i = 0; i < sz; i++)
                result[i] = readAnnotationValue(bytes);
            return result;
        case 'B': 
        case 'C': 
        case 'D': 
        case 'F': 
        case 'I': 
        case 'J': 
        case 'S': 
        case 'Z': 
        case 's': 
            int cp_index = bytes.readUnsignedShort();
            Constant c  =  getConstantPool().getConstant(cp_index);
	    switch(tag) {
        case 'B': 
		return new Byte(
            		(byte) ((ConstantInteger)c).getBytes());
        case 'C': 
		return new Character(
            		(char) ((ConstantInteger)c).getBytes());
        case 'D': 
		return new Double(
            		((ConstantDouble)c).getBytes());
        case 'F': 
		return new Float(
            		((ConstantFloat)c).getBytes());
        case 'I': 
		return new Integer(
            		((ConstantInteger)c).getBytes());
        case 'J': 
		return new Long(
            		((ConstantLong)c).getBytes());
        case 'S': 
		return new Character(
            		(char) ((ConstantInteger)c).getBytes());
        case 'Z': 
		return Boolean.valueOf(
            		((ConstantInteger)c).getBytes() != 0);
        case 's': 
            return ((ConstantUtf8)c).getBytes();
	default: throw new IllegalStateException("Impossible");
	}
        default: 
            throw new IllegalArgumentException();
        }
    }
}
