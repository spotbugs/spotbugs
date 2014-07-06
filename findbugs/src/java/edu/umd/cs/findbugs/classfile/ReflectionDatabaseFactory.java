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

package edu.umd.cs.findbugs.classfile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A generic database factory that tries to create the database by (in order of
 * preference)
 *
 * <ol>
 * <li>Invoking a static <b>create</b> method</li>
 * <li>Invoking a no-arg constructor
 * </ol>
 *
 * @author David Hovemeyer
 */
public class ReflectionDatabaseFactory<E> implements IDatabaseFactory<E> {
    private final Class<E> databaseClass;

    public ReflectionDatabaseFactory(Class<E> databaseClass) {
        this.databaseClass = databaseClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IDatabaseFactory#createDatabase()
     */
    @Override
    public E createDatabase() throws CheckedAnalysisException {
        E database;

        database = createUsingStaticCreateMethod();
        if (database != null) {
            return database;
        }

        database = createUsingConstructor();
        if (database != null) {
            return database;
        }

        throw new CheckedAnalysisException("Could not find a way to create database " + databaseClass.getName());
    }

    /**
     * Try to create the database using a static create() method.
     *
     * @return the database, or null if there is no static create() method
     * @throws CheckedAnalysisException
     */
    private E createUsingStaticCreateMethod() throws CheckedAnalysisException {
        Method createMethod;
        try {
            createMethod = databaseClass.getMethod("create", new Class[0]);
        } catch (NoSuchMethodException e) {
            return null;
        }

        if (!Modifier.isStatic(createMethod.getModifiers())) {
            return null;
        }

        if (createMethod.getReturnType() != databaseClass) {
            return null;
        }

        try {
            return databaseClass.cast(createMethod.invoke(null, new Object[0]));
        } catch (InvocationTargetException e) {
            throw new CheckedAnalysisException("Could not create " + databaseClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new CheckedAnalysisException("Could not create " + databaseClass.getName(), e);
        }
    }

    /**
     * Try to create the database using a no-arg constructor.
     *
     * @return the database, or null if there is no no-arg constructor
     * @throws CheckedAnalysisException
     */
    private E createUsingConstructor() throws CheckedAnalysisException {
        Constructor<E> constructor;
        try {
            constructor = databaseClass.getConstructor(new Class[0]);
        } catch (NoSuchMethodException e) {
            return null;
        }

        try {
            return constructor.newInstance(new Object[0]);
        } catch (InstantiationException e) {
            throw new CheckedAnalysisException("Could not create " + databaseClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new CheckedAnalysisException("Could not create " + databaseClass.getName(), e);
        } catch (InvocationTargetException e) {
            throw new CheckedAnalysisException("Could not create " + databaseClass.getName(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IDatabaseFactory#registerWith(edu.umd.cs
     * .findbugs.classfile.IAnalysisCache)
     */
    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerDatabaseFactory(databaseClass, this);
    }

}
