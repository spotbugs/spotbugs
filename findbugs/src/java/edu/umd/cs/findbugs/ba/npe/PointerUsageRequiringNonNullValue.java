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

package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;

/**
 * @author pugh
 */
public abstract class PointerUsageRequiringNonNullValue {

	public abstract String getDescription();

	public boolean isDirect() {
		return false;
	}

	public boolean getReturnFromNonNullMethod() {
		return false;
	}

	public @CheckForNull
	XMethodParameter getNonNullParameter() {
		return null;
	}

	public @CheckForNull
	XField getNonNullField() {
		return null;
	}

	private static final PointerUsageRequiringNonNullValue instance = new PointerUsageRequiringNonNullValue() {
		@Override
		public boolean isDirect() {
			return true;
		}

		@Override
		public String getDescription() {
			return  "SOURCE_LINE_DEREF";
		}
	};

	private static final PointerUsageRequiringNonNullValue nonNullReturnInstance = new PointerUsageRequiringNonNullValue() {
		@Override
		public boolean getReturnFromNonNullMethod() {
			return true;
		}

		@Override
		public String getDescription() {
			return  "SOURCE_LINE_RETURNED";
		}
	};

	public static PointerUsageRequiringNonNullValue getPointerDereference() {
		return instance;
	}

	public static PointerUsageRequiringNonNullValue getReturnFromNonNullMethod(XMethod m) {
		return nonNullReturnInstance;
	}

	public static PointerUsageRequiringNonNullValue getPassedAsNonNullParameter(final XMethod m, final int param) {
		return new PointerUsageRequiringNonNullValue() {
			@Override
			public @CheckForNull
			XMethodParameter getNonNullParameter() {
				return new XMethodParameter(m, param);
			}

			@Override
			public String getDescription() {
				return  "SOURCE_LINE_INVOKED";
			}

		};
	}

	public static PointerUsageRequiringNonNullValue getStoredIntoNonNullField(final XField f) {
		return new PointerUsageRequiringNonNullValue() {
			@Override
			public @CheckForNull
			XField getNonNullField() {
				return f;
			}

			@Override
			public String getDescription() {
				return  "SOURCE_LINE_STORED";
			}

		};
	}
}
