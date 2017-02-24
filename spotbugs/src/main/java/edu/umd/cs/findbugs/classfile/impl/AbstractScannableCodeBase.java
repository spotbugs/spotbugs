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
import java.util.Map;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;

/**
 * Abstract base class for implementations of IScannableCodeBase. Provides an
 * implementation of the getCodeBaseLocator(), containsSourceFiles(),
 * setApplicationCodeBase(), and isApplicationCodeBase() methods.
 *
 * @author David Hovemeyer
 */
public abstract class AbstractScannableCodeBase implements IScannableCodeBase {
    private final ICodeBaseLocator codeBaseLocator;

    private boolean isAppCodeBase;

    private ICodeBase.Discovered howDiscovered;

    private long lastModifiedTime;

    private final Map<String, String> resourceNameTranslationMap;

    public AbstractScannableCodeBase(ICodeBaseLocator codeBaseLocator) {
        this.codeBaseLocator = codeBaseLocator;
        this.lastModifiedTime = -1L;
        this.resourceNameTranslationMap = new HashMap<String, String>();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getCodeBaseLocator()
     */
    @Override
    public ICodeBaseLocator getCodeBaseLocator() {
        return codeBaseLocator;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IScannableCodeBase#containsSourceFiles()
     */
    @Override
    public boolean containsSourceFiles() {
        return false;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.ICodeBase#setApplicationCodeBase(boolean)
     */
    @Override
    public void setApplicationCodeBase(boolean isAppCodeBase) {
        this.isAppCodeBase = isAppCodeBase;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#isApplicationCodeBase()
     */
    @Override
    public boolean isApplicationCodeBase() {
        return isAppCodeBase;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#setHowDiscovered(int)
     */
    @Override
    public void setHowDiscovered(ICodeBase.Discovered howDiscovered) {
        this.howDiscovered = howDiscovered;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getHowDiscovered()
     */
    @Override
    public ICodeBase.Discovered getHowDiscovered() {
        return howDiscovered;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#setLastModifiedTime(long)
     */
    @Override
    public void setLastModifiedTime(long lastModifiedTime) {
        if (lastModifiedTime > 0 && FindBugs.validTimestamp(lastModifiedTime)) {
            this.lastModifiedTime = lastModifiedTime;
        }
    }
    public void addLastModifiedTime(long lastModifiedTime) {
        if (lastModifiedTime > 0 && FindBugs.validTimestamp(lastModifiedTime) && this.lastModifiedTime < lastModifiedTime) {
            this.lastModifiedTime = lastModifiedTime;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getLastModifiedTime()
     */
    @Override
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void addResourceNameTranslation(String origResourceName, String newResourceName) {
        if (!origResourceName.equals(newResourceName)) {
            resourceNameTranslationMap.put(origResourceName, newResourceName);
        }
    }

    public String translateResourceName(String resourceName) {
        String translatedName = resourceNameTranslationMap.get(resourceName);
        return translatedName != null ? translatedName : resourceName;
    }
}
