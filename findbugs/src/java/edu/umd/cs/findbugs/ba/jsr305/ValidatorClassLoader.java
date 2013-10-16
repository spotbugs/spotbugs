/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

/**
 * @author pugh
 */
class ValidatorClassLoader extends ClassLoader {

    ValidatorClassLoader() {
        super(ClassLoader.getSystemClassLoader().getParent());
        new RuntimeException("Creating ValidatorClassLoader #").printStackTrace();
    }
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("javax.annotation"))
                return Class.forName(name);
       
        try {
            System.out.println("Looking for class data for " + name);
            byte[] b = TypeQualifierValue.loadClassData(name);
            return findClass(name, b);
        } catch (CheckedAnalysisException e) {
            e.printStackTrace();
            return super.findClass(name);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }


    }

    public Class<?> findClass(String name, byte [] b) throws ClassNotFoundException {
        try {
            System.out.println("Loading " + b.length + " bytes for class " + name);
            Class<?> result = defineClass(name, b, 0, b.length);
            System.out.println("defined class");
            return result;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }


    }
    
    

}
