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

package edu.umd.cs.findbugs.classfile.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;

/**
 * Represents the class name, superclass name, and interface list of a class.
 *
 * @author David Hovemeyer
 */
public class ClassNameAndSuperclassInfo extends ClassDescriptor {
    private final ClassDescriptor superclassDescriptor;

    private final ClassDescriptor[] interfaceDescriptorList;

    private final ICodeBaseEntry codeBaseEntry;

    private final int accessFlags;

    private final Set<ClassDescriptor> calledClassDescriptors;

    private final int majorVersion, minorVersion;

    public static class Builder {
        ClassDescriptor classDescriptor;

        ClassDescriptor superclassDescriptor;

        ClassDescriptor[] interfaceDescriptorList;

        ICodeBaseEntry codeBaseEntry;

        int accessFlags;

        int majorVersion, minorVersion;

        Collection<ClassDescriptor> referencedClassDescriptorList;

        Set<ClassDescriptor> calledClassDescriptors = Collections.<ClassDescriptor> emptySet();

        public ClassNameAndSuperclassInfo build() {
            return new ClassNameAndSuperclassInfo(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry,
                    accessFlags, referencedClassDescriptorList, calledClassDescriptors, majorVersion, minorVersion);
        }

        /**
         * @param accessFlags
         *            The accessFlags to set.
         */
        public void setAccessFlags(int accessFlags) {
            this.accessFlags = accessFlags;
        }

        /**
         * @param classDescriptor
         *            The classDescriptor to set.
         */
        public void setClassDescriptor(ClassDescriptor classDescriptor) {
            this.classDescriptor = classDescriptor;
        }

        /**
         * @param codeBaseEntry
         *            The codeBaseEntry to set.
         */
        public void setCodeBaseEntry(ICodeBaseEntry codeBaseEntry) {
            this.codeBaseEntry = codeBaseEntry;
        }

        /**
         * @param interfaceDescriptorList
         *            The interfaceDescriptorList to set.
         */
        public void setInterfaceDescriptorList(ClassDescriptor[] interfaceDescriptorList) {
            this.interfaceDescriptorList = interfaceDescriptorList;
        }

        /**
         * @param superclassDescriptor
         *            The superclassDescriptor to set.
         */
        public void setSuperclassDescriptor(ClassDescriptor superclassDescriptor) {
            this.superclassDescriptor = superclassDescriptor;
        }

        public void setClassfileVersion(int majorVersion, int minorVersion) {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
        }

        /**
         * @param referencedClassDescriptorList
         *            The referencedClassDescriptorList to set.
         */
        public void setReferencedClassDescriptors(Collection<ClassDescriptor> referencedClassDescriptorList) {
            if (referencedClassDescriptorList.size() == 0) {
                this.referencedClassDescriptorList = Collections.emptyList();
            } else {
                this.referencedClassDescriptorList = new ArrayList<ClassDescriptor>(referencedClassDescriptorList);
            }
        }

        public void setCalledClassDescriptors(Collection<ClassDescriptor> calledClassDescriptorList) {
            if (calledClassDescriptorList.size() == 0) {
                this.calledClassDescriptors = Collections.emptySet();
            } else {
                this.calledClassDescriptors = new HashSet<ClassDescriptor>(calledClassDescriptorList);
            }
        }
    }


    ClassNameAndSuperclassInfo(ClassDescriptor classDescriptor, ClassDescriptor superclassDescriptor,
            ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags,
            /* TODO: We aren't doing anything with this */
            Collection<ClassDescriptor> referencedClassDescriptorList,
            @Nonnull Set<ClassDescriptor> calledClassDescriptors, int majorVersion, int minorVersion) {
        super(classDescriptor.getClassName());
        this.superclassDescriptor = superclassDescriptor;
        this.interfaceDescriptorList = interfaceDescriptorList;
        this.codeBaseEntry = codeBaseEntry;
        this.accessFlags = accessFlags;
        if (calledClassDescriptors == null) {
            throw new NullPointerException("calledClassDescriptors must not be null");
        }
        this.calledClassDescriptors = calledClassDescriptors;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;

    }

    /**
     * @return Returns the accessFlags.
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * @return Returns the majorVersion.
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * @return Returns the minorVersion.
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * @return Returns the classDescriptor.
     */
    public ClassDescriptor getClassDescriptor() {
        return this;
    }

    /**
     * @return Returns the codeBaseEntry.
     */
    public ICodeBaseEntry getCodeBaseEntry() {
        return codeBaseEntry;
    }

    /**
     * @return Returns the interfaceDescriptorList.
     */
    public ClassDescriptor[] getInterfaceDescriptorList() {
        return interfaceDescriptorList;
    }

    /**
     * @return Returns the called class descriptors.
     */
    public Set<ClassDescriptor> getCalledClassDescriptors() {
        return calledClassDescriptors;
    }

    /**
     * @return Returns the superclassDescriptor.
     */
    public ClassDescriptor getSuperclassDescriptor() {
        return superclassDescriptor;
    }

    private boolean isFlagSet(int flag) {
        return (getAccessFlags() & flag) != 0;
    }

    public boolean isFinal() {
        return isFlagSet(IClassConstants.ACC_FINAL);
    }

    public boolean isPrivate() {
        return isFlagSet(IClassConstants.ACC_PRIVATE);
    }

    public boolean isProtected() {
        return isFlagSet(IClassConstants.ACC_PROTECTED);
    }

    public boolean isPublic() {
        return isFlagSet(IClassConstants.ACC_PUBLIC);
    }

    public boolean isStatic() {
        return isFlagSet(IClassConstants.ACC_STATIC);
    }

    public boolean isInterface() {
        return isFlagSet(IClassConstants.ACC_INTERFACE);
    }

    public boolean isAbstract() {
        return isFlagSet(IClassConstants.ACC_ABSTRACT);
    }

    public boolean isAnnotation() {
        return isFlagSet(IClassConstants.ACC_ANNOTATION);
    }

    public boolean isSynthetic() {
        return isFlagSet(IClassConstants.ACC_SYNTHETIC);
    }

    public boolean isDeprecated() {
        return isFlagSet(Opcodes.ACC_DEPRECATED);
    }

}
