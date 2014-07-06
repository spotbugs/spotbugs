/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.classfile.IErrorLogger;

/**
 * An interface which Repository class lookup failures are reported to. Some of
 * the analysis classes make use of class hierarchy information. In collecting
 * this information, errors can result because some classes in the hierarchy
 * can't be found; e.g., when the runtime classpath is incomplete. When
 * possible, the analysis classes will be conservative in the event of a lookup
 * failure. However, it is important to report such lookup failures to the user.
 * So, classes that use the Repository should have a callback object to report
 * lookup failures to.
 *
 * @author David Hovemeyer
 */

// TODO: Rename this interface?
public interface RepositoryLookupFailureCallback extends IErrorLogger {
}

