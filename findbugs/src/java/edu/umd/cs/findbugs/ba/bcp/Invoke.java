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

package edu.umd.cs.findbugs.ba.bcp;

import java.util.regex.Pattern;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A PatternElement to match a method invocation.
 * Currently, we don't allow variables in this element (for arguments
 * and return value).  This would be a good thing to add.
 * We also don't distinguish between invokevirtual, invokeinterface,
 * and invokespecial.
 * <p/>
 * <p> Invoke objects match by class name, method name, method signature,
 * and <em>mode</em>.
 * <p/>
 * <p> Names and signatures may be matched in several ways:
 * <ol>
 * <li> By an exact match. This is the default behavior.
 * <li> By a regular expression. If the string provided to the Invoke
 * constructor begins with a "/" character, the rest of the string
 * is treated as a regular expression.
 * <li> As a subclass match. This only applies to class name matches.
 * If the first character of a class name string is "+", then the
 * rest of the string is treated as the name of a base class.
 * Any subclass or subinterface of the named type will be accepted.
 * </ol>
 * <p/>
 * <p> The <em>mode</em> specifies what kind of invocations in the Invoke
 * element matches. It is specified as the bitwise combination of the
 * following values:
 * <ol>
 * <li> <code>INSTANCE</code>, which matches ordinary instance method invocations
 * <li> <code>STATIC</code>, which matches static method invocations
 * <li> <code>CONSTRUCTOR</code>, which matches object constructor invocations
 * </ol>
 * The special mode <code>ORDINARY_METHOD</code> is equivalent to <code>INSTANCE|STATIC</code>.
 * The special mode <code>ANY</code> is equivalent to <code>INSTANCE|STATIC|CONSTRUCTOR</code>.
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public class Invoke extends PatternElement {

	/**
	 * Match ordinary (non-constructor) instance invocations.
	 */
	public static final int INSTANCE = 1;

	/**
	 * Match static invocations.
	 */
	public static final int STATIC = 2;

	/**
	 * Match object constructor invocations.
	 */
	public static final int CONSTRUCTOR = 4;

	/**
	 * Match ordinary methods (everything except constructors).
	 */
	public static final int ORDINARY_METHOD = INSTANCE | STATIC;

	/**
	 * Match both static and instance invocations.
	 */
	public static final int ANY = INSTANCE | STATIC | CONSTRUCTOR;

	private interface StringMatcher {
		public boolean match(String s);
	}

	private static class ExactStringMatcher implements StringMatcher {
		private String value;

		public ExactStringMatcher(String value) {
			this.value = value;
		}

		public boolean match(String s) {
			return s.equals(value);
		}
	}

	private static class RegexpStringMatcher implements StringMatcher {
		private Pattern pattern;

		public RegexpStringMatcher(String re) {
			pattern = Pattern.compile(re);
		}

		public boolean match(String s) {
			return pattern.matcher(s).matches();
		}
	}

	private static class SubclassMatcher implements StringMatcher {
		private String className;

		public SubclassMatcher(String className) {
			this.className = className;
		}

		public boolean match(String s) {
			try {
				return Hierarchy.isSubtype(s, className);
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
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
	 *
	 * @param className  the class name of the method; may be specified exactly,
	 *                   as a regexp, or as a subtype match
	 * @param methodName the name of the method; may be specified exactly or as a regexp
	 * @param methodSig  the signature of the method; may be specified exactly or as a regexp
	 * @param mode       the mode of invocation
	 */
	public Invoke(String className, String methodName, String methodSig, int mode,
				  @Nullable RepositoryLookupFailureCallback lookupFailureCallback) {
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

	@Override
		 public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
							 ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		// See if the instruction is an InvokeInstruction
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof InvokeInstruction))
			return null;
		InvokeInstruction inv = (InvokeInstruction) ins;

		String methodName = inv.getMethodName(cpg);
		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;
		boolean isCtor = methodName.equals("<init>");

		int actualMode = 0;

		if (isStatic) actualMode |= STATIC;
		if (isCtor) actualMode |= CONSTRUCTOR;
		if (!isStatic && !isCtor) actualMode |= INSTANCE;

		// Intersection of actual and desired modes must be nonempty.
		if ((actualMode & mode) == 0)
			return null;

		// Check class name, method name, and method signature.
		if (!methodNameMatcher.match(methodName) ||
				!methodSigMatcher.match(inv.getSignature(cpg)) ||
				!classNameMatcher.match(inv.getClassName(cpg)))
			return null;

		// It's a match!
		return new MatchResult(this, bindingSet);

	}

	@Override
		 public boolean acceptBranch(Edge edge, InstructionHandle source) {
		return true;
	}

	@Override
		 public int minOccur() {
		return 1;
	}

	@Override
		 public int maxOccur() {
		return 1;
	}
}

// vim:ts=4
