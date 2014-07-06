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
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pugh
 */
class ValidatorClassLoader extends ClassLoader {

    static {
        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            new RuntimeException("Initialising ValidatorClassLoader").printStackTrace();
        }

    }
    final static ValidatorClassLoader INSTANCE = new ValidatorClassLoader();

    ValidatorClassLoader() {
        super(ClassLoader.getSystemClassLoader().getParent());
        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            new RuntimeException("Creating ValidatorClassLoader #").printStackTrace();
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            if (resolve) {
                System.out.println("Loading and resolving class for " + name);
            } else {
                System.out.println("Loading class for " + name);
            }
        }

        return super.loadClass(name, resolve);
    }
    @Override
    public Class<?> findClass(@DottedClassName String name) throws ClassNotFoundException {
        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            System.out.println("Looking for class data for " + name);
        }

        if (name.startsWith("javax.annotation")) {
            return Class.forName(name);
        }

        try {
            byte[] b = TypeQualifierValue.loadClassData(name);
            return findClass(name, b);
        } catch (CheckedAnalysisException e) {
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                e.printStackTrace();
            }
            return super.findClass(name);
        } catch (RuntimeException e) {
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                e.printStackTrace();
            }
            throw e;
        }
    }


    private Class<?> findClass(@DottedClassName String name, byte [] b)  {
        try {
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                System.out.println("Loading " + b.length + " bytes for class " + name);
            }
            Class<?> result = defineClass(name, b, 0, b.length);
            super.resolveClass(result);
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                System.out.println("defined class " + name);
            }
            return result;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }


    }



}
