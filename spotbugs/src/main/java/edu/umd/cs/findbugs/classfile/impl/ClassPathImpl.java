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

package edu.umd.cs.findbugs.classfile.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of IClassPath.
 *
 * @author David Hovemeyer
 */
public class ClassPathImpl implements IClassPath {
    private final List<IScannableCodeBase> appCodeBaseList;

    private final List<ICodeBase> auxCodeBaseList;

    private final Map<String, ICodeBaseEntry> codeBaseEntryMap;

    public ClassPathImpl() {
        this.appCodeBaseList = new LinkedList<IScannableCodeBase>();
        this.auxCodeBaseList = new LinkedList<ICodeBase>();
        this.codeBaseEntryMap = new HashMap<String, ICodeBaseEntry>();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (IScannableCodeBase cb : appCodeBaseList) {
            buf.append(cb);
            buf.append(" ");
        }
        for (ICodeBase cb : auxCodeBaseList) {
            buf.append("*");
            buf.append(cb);
            buf.append(" ");
        }
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassPath#addCodeBase(edu.umd.cs.findbugs
     * .classfile.ICodeBase)
     */
    @Override
    public void addCodeBase(ICodeBase codeBase) {
        if (codeBase.isApplicationCodeBase()) {
            if (!(codeBase instanceof IScannableCodeBase)) {
                throw new IllegalStateException();
            }
            appCodeBaseList.add((IScannableCodeBase) codeBase);
        } else {
            auxCodeBaseList.add(codeBase);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IClassPath#appCodeBaseIterator()
     */
    @Override
    public Iterator<? extends ICodeBase> appCodeBaseIterator() {
        return appCodeBaseList.iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IClassPath#auxCodeBaseIterator()
     */
    @Override
    public Iterator<? extends ICodeBase> auxCodeBaseIterator() {
        return auxCodeBaseList.iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IClassPath#close()
     */
    @Override
    public void close() {
        for (ICodeBase codeBase : appCodeBaseList) {
            codeBase.close();
        }
        for (ICodeBase codeBase : auxCodeBaseList) {
            codeBase.close();
        }
        appCodeBaseList.clear();
        auxCodeBaseList.clear();
        codeBaseEntryMap.clear();
    }

    @Override
    public Map<String, ICodeBaseEntry> getApplicationCodebaseEntries() {
        Map<String, ICodeBaseEntry> appEntries = new HashMap<String, ICodeBaseEntry>();
        Iterator<Entry<String, ICodeBaseEntry>> iterator = codeBaseEntryMap.entrySet().iterator();
        while(iterator.hasNext()) {
            Entry<String, ICodeBaseEntry> entry = iterator.next();
            ICodeBaseEntry codeBaseEntry = entry.getValue();
            if(codeBaseEntry.getCodeBase().isApplicationCodeBase()) {
                appEntries.put(entry.getKey(), codeBaseEntry);
            }
        }
        return appEntries;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassPath#lookupResource(java.lang.String)
     */
    @Override
    public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
        // See if we have cached the codebase entry for this resource
        ICodeBaseEntry result = codeBaseEntryMap.get(resourceName);

        if (result == null) {
            // No previously resolved entry - look up the resources in the
            // codebases

            // First try application codebases
            result = search(appCodeBaseList, resourceName);
            if (result == null) {
                // Next try aux codebases
                result = search(auxCodeBaseList, resourceName);
            }

            // If not found in any codebase, then throw
            // ResourceNotFoundException
            if (result == null) {
                throw new ResourceNotFoundException(resourceName);
            }

            // Cache the entry for future lookups
            codeBaseEntryMap.put(resourceName, result);
        }

        return result;
    }

    /**
     * Search list of codebases for named resource.
     *
     * @param codeBaseList
     *            list of codebases to search
     * @param resourceName
     *            name of resourse
     * @return codebase entry for the named resource, or null if the named
     *         resource cannot be found
     */
    private ICodeBaseEntry search(List<? extends ICodeBase> codeBaseList, String resourceName) {
        for (ICodeBase codeBase : codeBaseList) {
            ICodeBaseEntry resource = codeBase.lookupResource(resourceName);
            if (resource != null) {
                return resource;
            }
            // Ignore, continue trying other codebases
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassPath#mapResourceNameToCodeBaseEntry
     * (java.lang.String, edu.umd.cs.findbugs.classfile.ICodeBaseEntry)
     */
    @Override
    public void mapResourceNameToCodeBaseEntry(String resourceName, ICodeBaseEntry codeBaseEntry) {
        codeBaseEntryMap.put(resourceName, codeBaseEntry);
    }
}
