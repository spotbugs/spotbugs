/*
 * SpotBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.detect.ReflectiveAccessTracker.AccessType;

/**
 * Handles and logs reflective access to a field.
 */
class ReflectiveFieldAccessor {

    private final AccessType accessType;
    private final ReflectiveFieldAccessLog accessLog;
    private final SourceLineAnnotation declarationSourceLine;
    private boolean wasUsed = false;

    public ReflectiveFieldAccessor(final ReflectiveFieldAccessLog accessLog, final AccessType accessType,
            final SourceLineAnnotation declarationSourceLine) {
        this.accessType = accessType;
        this.accessLog = accessLog;
        this.declarationSourceLine = declarationSourceLine;
    }

    public ReflectiveFieldAccessLog getReflectiveAccessLog() {
        return accessLog;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public SourceLineAnnotation getDeclarationSourceLine() {
        return declarationSourceLine;
    }

    public boolean wasUsed() {
        return wasUsed;
    }

    protected void markUsed() {
        wasUsed = true;
    }

    /**
     * Accessor which can be invoked to both set and get the value. The accessType is supplied at invocation.
     */
    static class ExplicitAccessor extends ReflectiveFieldAccessor {

        public ExplicitAccessor(final ReflectiveFieldAccessLog reflectiveAccessLog,
                final SourceLineAnnotation declarationSourceLine) {
            super(reflectiveAccessLog, AccessType.BOTH, declarationSourceLine);
        }

        public void markAccess(final AccessType accessType) {
            markUsed();
            getReflectiveAccessLog().markAccess(accessType);
        }
    }

    /**
     * Accessor which can access a field only in pre-defined way. The accessType is defined at instantiation.
     */
    static class ImplicitAccessor extends ReflectiveFieldAccessor {

        public ImplicitAccessor(final ReflectiveFieldAccessLog reflectiveAccessLog,
                final AccessType accessType, final SourceLineAnnotation declarationSourceLine) {
            super(reflectiveAccessLog, accessType, declarationSourceLine);
        }

        public void markAccess() {
            markUsed();
            getReflectiveAccessLog().markAccess(getAccessType());
        }
    }
}
