/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba.bcp;

import java.util.regex.*;
import org.apache.bcel.Repository;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import edu.umd.cs.daveho.ba.*;

/**
 * A PatternElement to match a method invocation.
 * Currently, we don't allow variables in this element (for arguments
 * and return value).  This would be a good thing to add.
 * We also don't distinguish between invokevirtual, invokeinterface,
 * and invokespecial.
 *
 * <p> Invoke objects match by class name, method name, method signature,
 * and static/nonstatic status.
 *
 * <p> Names and signatures may be matched in several ways:
 * <ol>
 * <li> By an exact match. This is the default behavior.
 * <li> By a regular expression. If the string provided to the Invoke
 *   constructor begins with a "/" character, the rest of the string
 *   is treated as a regular expression.
 * <li> As a subclass match. This only applies to class name matches.
 *   If the first character of a class name string is "+", then the
 *   rest of the string is treated as the name of a base class.
 *   Any subclass or subinterface of the named type will be accepted.
 * </ol>
 *
 * <p> Static/nonstatic status is specified as the <em>mode</em> of
 * the Invoke object.  This can have three values:
 * <ol>
 * <li> <code>STATIC</code>. Only static invocations are matched.
 * <li> <code>INSTANCE</code>. Only non-static (instance) invocations are matched.
 * <li> <code>ANY</code>. Both static and instance invocations are matched.
 * </ol>
 *
 * @see PatternElement
 * @author David Hovemeyer
 */
public class Invoke extends PatternElement {

	/** Match only static invocations. */
	public static final int STATIC = 0;

	/** Match only non-static (instance) invocations. */
	public static final int INSTANCE = 1;

	/** Match both static and instance invocations. */
	public static final int ANY = 2;

	private interface StringMatcher {
		public boolean match(String s);
	}

	private static class ExactStringMatcher implements StringMatcher {
		private String value;
		public ExactStringMatcher(String value) { this.value = value; }
		public boolean match(String s) { return s.equals(value); }
	}

	private static class RegexpStringMatcher implements StringMatcher {
		private Pattern pattern;
		public RegexpStringMatcher(String re) {
			pattern = Pattern.compile(re);
		}
		public boolean match(String s) { return pattern.matcher(s).matches(); }
	}

	private static class SubclassMatcher implements StringMatcher {
		private String className;
		public SubclassMatcher(String className) { this.className = className; }
		public boolean match(String s) {
			try {
				return Repository.instanceOf(s, className);
			} catch (ClassNotFoundException e) {
				return false;
			}
		}
	}

	private final StringMatcher classNameMatcher;
	private final StringMatcher methodNameMatcher;
	private final StringMatcher methodSigMatcher;
	private final int mode;

	/**
	 * Constructor.
	 * @param className the class name of the method; may be specified exactly,
	 *   as a regexp, or as a subtype match
	 * @param methodName the name of the method; may be specified exactly or as a regexp
	 * @param methodSig the signature of the method; may be specified exactly or as a regexp
	 * @param mode one of STATIC, INSTANCE, or ANY; specifies whether we should
	 *  match static methods only, instance methods only, or either
	 */
	public Invoke(String className, String methodName, String methodSig, int mode) {
		this.classNameMatcher = createClassMatcher(className);
		this.methodNameMatcher = createMatcher(methodName);
		this.methodSigMatcher = createMatcher(methodSig);
		this.mode = mode;
	}

	private StringMatcher createClassMatcher(String s) {
		return s.startsWith("+")
			? new SubclassMatcher(s.substring(1))
			: createMatcher(s);
	}

	private StringMatcher createMatcher(String s) {
		return s.startsWith("/")
			? (StringMatcher) new RegexpStringMatcher(s.substring(1))
			: (StringMatcher) new ExactStringMatcher(s);
	}

	public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
		ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		// See if the instruction is an InvokeInstruction
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof InvokeInstruction))
			return null;
		InvokeInstruction inv = (InvokeInstruction) ins;

		// Check that it's static or non-static, as appropriate
		if (mode != ANY) {
			boolean isStatic = (inv.getOpcode() == Constants.INVOKESTATIC);
			boolean wantStatic = (mode == STATIC);
			if (isStatic != wantStatic)
				return null;
		}

		// Check class name, method name, and method signature.
		if (!classNameMatcher.match(inv.getClassName(cpg)) ||
			!methodNameMatcher.match(inv.getMethodName(cpg)) ||
			!methodSigMatcher.match(inv.getSignature(cpg)))
			return null;

		// It's a match!
		return new MatchResult(this, bindingSet);

	}

	public boolean acceptBranch(Edge edge, InstructionHandle source) {
		return true;
	}

	public int minOccur() {
		return 1;
	}

	public int maxOccur() {
		return 1;
	}
}

// vim:ts=4
