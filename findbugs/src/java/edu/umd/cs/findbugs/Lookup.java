package edu.umd.cs.findbugs;

import java.io.*;
import java.util.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import org.apache.bcel.classfile.*;
import org.apache.bcel.Repository;

public class Lookup 
	implements Constants2
{
  public static JavaClass 
	findSuperImplementor(JavaClass clazz, String name, String signature) {
		JavaClass c = 
			findImplementor(Repository.getSuperClasses(clazz),
				name, signature);
		if (c != null) return c;
		return clazz;
		}
  public static String 
	findSuperImplementor(String clazz, String name, String signature) {
		JavaClass c = 
			findImplementor(Repository.getSuperClasses(clazz),
				name, signature);
		if (c != null) return c.getClassName();
		return clazz;
		}
  public static JavaClass 
	findImplementor(JavaClass[] clazz, String name, String signature) {
	
	for(int i = 0;i < clazz.length;i++) {
		Method m = findImplementation(clazz[i], name, signature);
		if (m != null) {
		      if ((m.getAccessFlags() & ACC_ABSTRACT) == 1)
			return null;
		      else return clazz[i];
		      }
		}
	return null;
	}
  public static Method 
	findImplementation(JavaClass clazz, String name, String signature) {
		Method[] m = clazz.getMethods();
		for(int i = 0; i < m.length; i++) 
			if (m[i].getName().equals(name)
			    && m[i].getSignature().equals(signature))
			  return m[i];
		return null;
		}
}
