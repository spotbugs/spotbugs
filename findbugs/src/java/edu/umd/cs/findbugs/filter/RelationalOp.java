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

package edu.umd.cs.findbugs.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pugh
 */
public abstract class RelationalOp {

	public abstract <T extends Comparable<T>> boolean check(T x, T y);

	final String value;
	final String name;

	private static final Map<String, RelationalOp> map = new HashMap<String, RelationalOp>();
	public static RelationalOp byName(String s) {
		RelationalOp relationalOp = map.get(s);
		if (relationalOp == null) 
			throw new IllegalArgumentException("Could not find relOp named " + s + " in " + map.keySet());
		return relationalOp;
	}
	public static Collection<RelationalOp> values() {
		return map.values();
	}
	/**
     * @deprecated Use {@link #RelationalOp(String,String)} instead
     */
	@Deprecated
    private RelationalOp(String value) {
        this(value, "xxx");
    }
	private RelationalOp(String value, String name) {
		this.value = value;
		this.name = name;
		map.put(name, this);
	}

	@Override
    public String toString() {
		return value;
	}
	public String getName() {
		return name;
	}

	public static final RelationalOp EQ = new RelationalOp("==", "EQ") {
		@Override
        public <T extends Comparable<T>> boolean check(T x, T y) {
			return x.compareTo(y) == 0;
		}

	};

	public static final RelationalOp LEQ = new RelationalOp("<=", "LEQ") {
		@Override
        public <T extends Comparable<T>> boolean check(T x, T y){
			return x.compareTo(y) <= 0;
		}

	};

	public static final RelationalOp NEQ = new RelationalOp("!=", "NEQ") {
		@Override
        public <T extends Comparable<T>> boolean check(T x, T y) {
			return x.compareTo(y) != 0;
		}

	};

	public static final RelationalOp GEQ = new RelationalOp(">=", "GEQ") {
		@Override
        public <T extends Comparable<T>> boolean check(T x, T y){
			return x.compareTo(y) >= 0;
		}

	};

	public static final RelationalOp LT = new RelationalOp("<", "LT") {
		@Override
        public <T extends Comparable<T>> boolean check(T x, T y) {
			return x.compareTo(y) < 0;
		}

	};

	public static final RelationalOp GT = new RelationalOp(">", "GT") {
		@Override
        public <T extends Comparable<T>> boolean check(T x, T y) {
			return x.compareTo(y) > 0;
		}

	};

}
