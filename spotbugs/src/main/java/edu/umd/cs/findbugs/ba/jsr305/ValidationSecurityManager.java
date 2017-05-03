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

import java.lang.annotation.Annotation;
import java.security.Permission;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

final class ValidationSecurityManager extends SecurityManager {


    static final ValidationSecurityManager INSTANCE = new ValidationSecurityManager();
    final static ValidatorClassLoader VALIDATOR_LOADER = ValidatorClassLoader.INSTANCE;


    static {
        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            new RuntimeException("Creating ValidationSecurityManager #").printStackTrace();
        }

    }
    public static <A extends Annotation> When sandboxedValidation(A proxy, TypeQualifierValidator<A> v, @CheckForNull
            Object constantValue) {
        if (performingValidation.get()) {
            throw new IllegalStateException("recursive validation");
        }

        try {
            performingValidation.set(Boolean.TRUE);
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                System.out.println("Performing validation in thread " + Thread.currentThread().getName());
            }
            try {
                When result = v.forConstantValue(proxy, constantValue);
                if (!performingValidation.get()) {
                    throw new IllegalStateException("performingValidation not set when validation completes");
                }
                return result;
            } catch (ClassCastException e) {
                Class<? extends Annotation> c = proxy.getClass();
                System.out.println(c.getName() + " extends " + c.getSuperclass().getName());
                for(Class<?> i : c.getInterfaces()) {
                    System.out.println("  " + i.getName());
                }
                throw e;
            }

        } finally {
            performingValidation.set(Boolean.FALSE);
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                System.out.println("Validation finished in thread " + Thread.currentThread().getName());
            }

        }
    }


    @Override
    public void checkPermission(Permission perm) {
        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            System.out.println("Checking for " + perm + " permission in thread " + Thread.currentThread().getName());
        }
        if (performingValidation.get() && inValidation()) {
            SecurityException e = new SecurityException("No permissions granted while performing JSR-305 validation");
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                e.printStackTrace(System.out);
            }
            throw e;
        }

    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        if (TypeQualifierValue.DEBUG_CLASSLOADING) {
            System.out.println("Checking for " + perm + " permission with content in thread " + Thread.currentThread().getName());
        }

        if (performingValidation.get() && inValidation()) {
            SecurityException e = new SecurityException("No permissions granted while performing JSR-305 validation");
            if (TypeQualifierValue.DEBUG_CLASSLOADING) {
                e.printStackTrace(System.out);
            }
            throw e;
        }
    }

    private ValidationSecurityManager() { }

    private boolean inValidation() {
        for (Class<?> c : getClassContext()) {
            if (TypeQualifierValidator.class.isAssignableFrom(c)
                    || c.getClassLoader() == VALIDATOR_LOADER) {
                return true;
            }
        }
        return false;
    }

    private static final ThreadLocal<Boolean> performingValidation = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }

    };


}
