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
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.detect.ReflectiveAccessTracker.AccessType;

/**
 * A record of read/write access of a field
 */
class ReflectiveFieldAccessLog {

    private final XField actualField;
    private boolean setterInvoked = false;
    private boolean getterInvoked = false;
    private SourceLineAnnotation sourceLine;

    public ReflectiveFieldAccessLog(final XField actualField) {
        this.actualField = actualField;
    }

    public XField getActualField() {
        return actualField;
    }

    public boolean wasSetterInvoked() {
        return setterInvoked;
    }

    public boolean wasGetterInvoked() {
        return getterInvoked;
    }

    public void setSourceLineIfAbsent(final SourceLineAnnotation annotation) {
        if (this.sourceLine == null) {
            this.sourceLine = annotation;
        }
    }

    public SourceLineAnnotation getSourceLine() {
        return sourceLine;
    }

    public void markAccess(final AccessType accessType) {
        switch (accessType) {
        case GETTER:
            getterInvoked = true;
            break;
        case SETTER:
            setterInvoked = true;
            break;
        case BOTH:
            getterInvoked = setterInvoked = true;
        }
    }
}
