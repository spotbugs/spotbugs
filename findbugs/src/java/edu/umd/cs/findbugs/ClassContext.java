/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * Object which creates and caches MethodGen and CFG objects for a class,
 * so they don't have to be created repeatedly by different visitors.
 * This results in a small but significant speedup when running multiple
 * visitors which use CFGs.
 */
public class ClassContext {
	private static class CFGKey {
		public final Method method;
		public final int mode;

		public CFGKey(Method method, int mode) {
			this.method = method;
			this.mode = mode;
		}

		public boolean equals(Object o) {
			CFGKey other = (CFGKey) o;
			return method == other.method && mode == other.mode;
		}

		public int hashCode() {
			return System.identityHashCode(method) + mode;
		}
	}

	private JavaClass jclass;
	private IdentityHashMap<Method, MethodGen> methodGenMap = new IdentityHashMap<Method, MethodGen>();
	private HashMap<CFGKey, CFG> cfgMap = new HashMap<CFGKey, CFG>();
	private ClassGen classGen;

	/**
	 * Constructor.
	 * @param jclass the JavaClass
	 */
	public ClassContext(JavaClass jclass) {
		this.jclass = jclass;
		this.classGen = null;
	}

	/**
	 * Get the JavaClass.
	 */
	public JavaClass getJavaClass() { return jclass; }

	/**
	 * Get a MethodGen object for given method.
	 * @param method the method
	 * @return the MethodGen object for the method
	 */
	public MethodGen getMethodGen(Method method) {
		MethodGen methodGen = methodGenMap.get(method);
		if (methodGen == null) {
			if (classGen == null)
				classGen = new ClassGen(jclass);
			ConstantPoolGen cpg = classGen.getConstantPool();
			methodGen = new MethodGen(method, jclass.getClassName(), cpg);
			methodGenMap.put(method, methodGen);
		}
		return methodGen;
	}

	/**
	 * Get a CFG for given method.
	 * @param method the method
	 * @param mode the CFGBuilder mode; see {@link CFGBuilderModes}
	 * @return the CFG
	 */
	public CFG getCFG(Method method, int mode) {
		CFGKey key = new CFGKey(method, mode);
		CFG cfg = cfgMap.get(key);
		if (cfg == null) {
			MethodGen methodGen = getMethodGen(method);
			BasicCFGBuilder cfgBuilder = new BasicCFGBuilder(methodGen);
			cfgBuilder.setMode(mode);
			cfgBuilder.build();
			cfg = cfgBuilder.getCFG();
			cfg.assignEdgeIds(0);
			cfgMap.put(key, cfg);
		}
		return cfg;
	}
}

// vim:ts=4
