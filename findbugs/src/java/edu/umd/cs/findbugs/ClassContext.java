package edu.umd.cs.visitclass;

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
