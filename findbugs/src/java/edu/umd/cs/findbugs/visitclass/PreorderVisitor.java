/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.pugh.visitclass;
import org.apache.bcel.classfile.*;

/**
 * Interface to make the use of a visitor pattern programming style possible.
 * I.e. a class that implements this interface can traverse the contents of
 * a Java class just by calling the `accept' method which all classes have.
 *
 * Implemented by wish of 
 * <A HREF="http://www.inf.fu-berlin.de/~bokowski">Boris Bokowski</A>.
 *
 * If don't like it, blame him. If you do like it thank me 8-)
 *
 * @version 970819
 * @author  <A HREF="http://www.inf.fu-berlin.de/~dahm">M. Dahm</A>
 */
public abstract class PreorderVisitor extends BetterVisitor implements Constants2 {

  // Attributes
	 public void visitCode(Code obj) { 
	super.visitCode(obj);
	CodeException[] exceptions = obj.getExceptionTable();
	for(int i = 0; i < exceptions.length; i++) 
		exceptions[i].accept(this);
	Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++)
	       attributes[i].accept(this);
	}

  // Constants
	 public void visitConstantPool(ConstantPool obj)    {
	super.visitConstantPool(obj);
	Constant[] constant_pool = obj.getConstantPool();
	for(int i = 1; i < constant_pool.length; i++) {
	    constant_pool[i].accept(this);
	    byte tag = constant_pool[i].getTag();
	    if((tag == CONSTANT_Double) || (tag == CONSTANT_Long))
		i++;
	    }
	}
	 public void visitField(Field obj)     {
	super.visitField(obj);
        Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++)
	      attributes[i].accept(this);
	}

  // Extra classes (i.e. leaves in this context)
	 public void visitInnerClasses(InnerClasses obj)     {
	super.visitInnerClasses(obj);
	InnerClass[] inner_classes = obj.getInnerClasses();
	for(int i=0; i < inner_classes.length; i++)
	      inner_classes[i].accept(this);
	}

	 public void visitAfter(JavaClass obj)  {}

  // General classes
	 public void visitJavaClass(JavaClass obj)     {
	super.visitJavaClass(obj);
        constant_pool.accept(this);
	Field[] fields = obj.getFields();
	Method[] methods = obj.getMethods();
	Attribute[] attributes = obj.getAttributes();
        for(int i = 0; i < fields.length; i++) fields[i].accept(this);
        for(int i = 0; i < methods.length; i++) methods[i].accept(this);
        for(int i = 0; i < attributes.length; i++) attributes[i].accept(this);
	visitAfter(obj);
	}

	 public void visitLineNumberTable(LineNumberTable obj)    {
	super.visitLineNumberTable(obj);
	LineNumber[] line_number_table = obj.getLineNumberTable();
	for(int i=0; i < line_number_table.length; i++)
	      line_number_table[i].accept(this);
	}

	 public void visitLocalVariableTable(LocalVariableTable obj)     {
	super.visitLocalVariableTable(obj);
	LocalVariable[] local_variable_table = obj.getLocalVariableTable();
	for(int i=0; i < local_variable_table.length; i++)
	      local_variable_table[i].accept(this);
	}

	 public void visitMethod(Method obj)     {
	super.visitMethod(obj);
        Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++)
	      attributes[i].accept(this);
	}
}
