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

package edu.umd.cs.findbugs.ba.npe2;

/**
 * Symbolic values representing the nullness of a runtime value.
 * 
 * @author David Hovemeyer
 */
public class NullnessValue {
	static final int DEFINITELY_NULL = 0;
	static final int DEFINITELY_NOT_NULL = 1;
	static final int CHECKED = 2;
	static final int NO_KABOOM = 3;

	static final int FLAGS_MAX = 4;

	private static final NullnessValue[] instanceList = new NullnessValue[1 << FLAGS_MAX];
	static {
		for (int i = 0; i < instanceList.length; i++) {
			instanceList[i] = new NullnessValue(i);
		}
	}

	private final int flags;

	private NullnessValue(int flags) {
		this.flags= flags;
	}

	int getFlags() {
		return flags;
	}

	public boolean isDefinitelyNull() {
		return isFlagSet(DEFINITELY_NULL);
	}

	public boolean isDefinitelyNotNull() {
		return isFlagSet(DEFINITELY_NOT_NULL);
	}

	public boolean isChecked() {
		return isFlagSet(CHECKED);
	}

	public boolean isNoKaboom() {
		return isFlagSet(NO_KABOOM);
	}

	public NullnessValue toCheckedValue() {
		return instanceList[flags | (1 << CHECKED)];
	}

	public NullnessValue toNoKaboomValue() {
		return instanceList[flags | (1 << NO_KABOOM)];
	}

//	public NullnessValue toCheckedNullValue() {
//		if (isDefinitelyNull() || isDefinitelyNotNull()) {
//			throw new IllegalStateException();
//		}
//		
//		return fromFlags(flags | DEFINITELY_NULL | CHECKED);
//	}
//	
//	public NullnessValue toCheckedNotNullValue() {
//		if (isDefinitelyNull() || isDefinitelyNotNull()) {
//			throw new IllegalStateException();
//		}
//
//		return fromFlags(flags | DEFINITELY_NOT_NULL | CHECKED);
//	}

	private boolean isFlagSet(int flag) {
		return (flags & (1 << flag)) != 0;
	}

	static NullnessValue fromFlags(int flags) {
		return instanceList[flags];
	}

	public static NullnessValue definitelyNullValue() {
		return fromFlags(1 << DEFINITELY_NULL);
	}

	public static NullnessValue definitelyNotNullValue() {
		return fromFlags(1 << DEFINITELY_NOT_NULL);
	}

	public static NullnessValue unknownValue() {
		return fromFlags(0);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String pfx = "";

		if (isChecked()) {
			pfx += "c";
		}

		if (isNoKaboom()) {
			pfx += "k";
		}

		String val;

		if (isDefinitelyNull()) {
			val = "n";
		} else if (isDefinitelyNotNull()) {
			val = "N";
		} else {
			val = "-";
		}

		return pfx + val;
	}
}
