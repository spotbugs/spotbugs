/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.util.ArrayList;
import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.AnalysisLocal;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ByteCodePatternDetector;
import edu.umd.cs.findbugs.JavaVersion;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePattern;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePatternMatch;
import edu.umd.cs.findbugs.ba.bcp.Invoke;
import edu.umd.cs.findbugs.ba.bcp.MatchAny;
import edu.umd.cs.findbugs.ba.bcp.Opcode;
import edu.umd.cs.findbugs.ba.bcp.PatternElement;

/**
 * This detector looks for places where the return value of a method
 * is suspiciously ignored.  Ignoring the return values from immutable
 * objects such as java.lang.String are a common and easily found type of bug.
 *
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public @Deprecated class BCPMethodReturnCheck extends ByteCodePatternDetector  {
	private final BugReporter bugReporter;

	private static final boolean CHECK_ALL = SystemProperties.getBoolean("mrc.checkall");

	private static AnalysisLocal<ByteCodePattern> localByteCodePattern
			= new AnalysisLocal<ByteCodePattern>();

		private static AnalysisLocal<ArrayList<PatternElement>> localPatternElementList
			= new AnalysisLocal<ArrayList<PatternElement>>();




	@Override
		 public ByteCodePattern  getPattern() {
		ByteCodePattern  result = localByteCodePattern.get();
		if (result == null) {
			ArrayList<PatternElement> list = getPatternElementList();
			PatternElement [] calls = list.toArray(new PatternElement[list.size()]);
			// The ByteCodePattern which specifies the kind of code pattern
			// we're looking for.  We want to match the invocation of certain methods
			// followed by a POP or POP2 instruction.
			result = new ByteCodePattern()
				.add(new MatchAny(calls).label("call").setAllowTrailingEdges(false))
				.add(new MatchAny(new PatternElement[]{new Opcode(Constants.POP), new Opcode(Constants.POP2)}));
			localByteCodePattern.set(result);
			}
		return result;
		}

	public static  void
		addMethodWhoseReturnMustBeChecked(String className, String methodName, 
				String methodSig, int mode) {
		ArrayList<PatternElement> list = getPatternElementList();
		list.add(new Invoke(className, methodName, methodSig, mode, null));
		localByteCodePattern.remove();
		}

	/**
	 * Return List of PatternElement objects representing
	 * method invocations requiring a return value check.
	 */
	private static 
		ArrayList<PatternElement> getPatternElementList() {
		ArrayList<PatternElement> list = localPatternElementList.get();
		if (list != null) return list;

		list = new ArrayList<PatternElement>();

		// Standard return check methods
		list.add(new Invoke("/.*", "equals",
				"/\\(Ljava/lang/Object;\\)Z",
				Invoke.INSTANCE, null));
		list.add(new Invoke("java.lang.String", "/.*",
				"/\\(.*\\)Ljava/lang/String;",
				Invoke.INSTANCE, null));
		list.add(new Invoke("java.lang.StringBuffer", "toString",
				"()Ljava/lang/String;",
				Invoke.INSTANCE,
				null));
		list.add(new Invoke("+java.lang.Thread", "<init>",
				"/.*",
				Invoke.CONSTRUCTOR,
				null));
		list.add(new Invoke("+java.lang.Throwable", "<init>",
				"/.*",
				Invoke.CONSTRUCTOR,
				null));
		list.add(new Invoke("java.security.MessageDigest",
				"digest", "([B)[B",
				Invoke.INSTANCE, null));
		list.add(new Invoke("+java.sql.Connection", "/.*", "/.*",
				Invoke.INSTANCE, null));
//		list.add(new Invoke("+java.net.InetAddress", "/.*", "/.*",
//		        Invoke.INSTANCE, null));
		list.add(new Invoke("java.math.BigDecimal", "/.*", "/.*",
				Invoke.INSTANCE, null));
		list.add(new Invoke("java.math.BigInteger", "/.*", "/.*",
				Invoke.INSTANCE, null));
		list.add(new Invoke("+java.util.Enumeration", "hasMoreElements", "()Z", Invoke.INSTANCE, null));
		list.add(new Invoke("+java.util.Iterator", "hasNext", "()Z", Invoke.INSTANCE, null));
		list.add(new Invoke("java.io.File", "createNewFile", "()Z", Invoke.INSTANCE, null));

		if (CHECK_ALL ||
				JavaVersion.getRuntimeVersion().isSameOrNewerThan(JavaVersion.JAVA_1_5)) {
			// Add JDK 1.5 and later return check functions
			list.add(new Invoke("+java.util.concurrent.locks.ReadWriteLock",
					"readLock",
					"()Ljava/util/concurrent/locks/Lock;",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.ReadWriteLock",
					"writeLock",
					"()Ljava/util/concurrent/locks/Lock;",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.Condition",
					"await",
					"(JLjava/util/concurrent/TimeUnit;)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.Condition",
					"awaitUtil",
					"(Ljava/util/Date;)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.Condition",
					"awaitNanos",
					"(J)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.Semaphore",
					"tryAcquire",
					"(JLjava/util/concurrent/TimeUnit;)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.Semaphore",
					"tryAcquire",
					"()Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.Lock",
					"tryLock",
					"(JLjava/util/concurrent/TimeUnit;)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.Lock",
					"newCondition",
					"()Ljava/util/concurrent/locks/Condition;",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.locks.Lock",
					"tryLock",
					"()Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.Queue",
					"offer",
					"(Ljava/lang/Object;)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.BlockingQueue",
					"offer",
					"(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.concurrent.BlockingQueue",
					"poll",
					"(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;",
					Invoke.INSTANCE,
					null));
			list.add(new Invoke("+java.util.Queue",
					"poll",
					"()Ljava/lang/Object;",
					Invoke.INSTANCE,
					null));
		}


		String externalCheckReturnValues = SystemProperties.getProperty("checkReturnValues");
		if (externalCheckReturnValues != null) {
			String [] checks = externalCheckReturnValues.split("[|]");
			for (String check : checks) {
				String [] parts = check.split(":");
				if (parts.length != 3) continue;
				Invoke in =
						new Invoke(parts[0], parts[1], parts[2], Invoke.INSTANCE, null);
				list.add(in);
			}
			}


		localPatternElementList.set(list);
		return list;
	}

	/**
	 * Constructor.
	 *
	 * @param bugReporter the BugReporter to report bug instances with
	 */
	public BCPMethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;

	}


	@Override
		 protected BugReporter getBugReporter() {
		return bugReporter;
	}


	@Override
		 public boolean prescreen(Method method, ClassContext classContext) {
		// Pre-screen for methods with POP or POP2 bytecodes.
		// This gives us a speedup close to 5X.
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet != null && (bytecodeSet.get(Constants.POP) || bytecodeSet.get(Constants.POP2));
	}

	@Override
		 public void reportMatch(ClassContext classContext, Method method, ByteCodePatternMatch match) {
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		JavaClass javaClass = classContext.getJavaClass();

		InstructionHandle call = match.getLabeledInstruction("call");

		// Ignore inner-class access methods
		InvokeInstruction inv = (InvokeInstruction) call.getInstruction();
		ConstantPoolGen cp = methodGen.getConstantPool();
		String calledMethodName = inv.getMethodName(cp);
		if (calledMethodName.startsWith("access$")
				|| calledMethodName.startsWith("access+"))
			return;

		/*
		System.out.println("Found " + calledMethodName);
		System.out.println(inv.getSignature(cp));
		System.out.println(inv.getClassName(cp));
		*/
		String calledMethodClass = inv.getClassName(cp);
		if (inv.getSignature(cp).endsWith("V") && !calledMethodName.equals("<init>"))
			return;
		/*
		if (calledMethodClass.equals(javaClass.getClassName()))
			return;
		*/
		String sourceFile = javaClass.getSourceFileName();
		/*
		System.out.println("CalledMethodClass: " + calledMethodClass);
		System.out.println("CalledMethodName: " + calledMethodName);
		*/
		int priority = HIGH_PRIORITY;
		if (calledMethodName.equals("createNewFile"))
			priority = LOW_PRIORITY;

		if ( calledMethodClass.startsWith("java.lang")
			|| calledMethodClass.startsWith("java.math")
				|| calledMethodClass.endsWith("Error")
				|| calledMethodClass.endsWith("Exception"))
			priority--;
		if (calledMethodClass.equals(javaClass.getClassName()))
			priority++;
		String calledPackage = extractPackageName(calledMethodClass);
		String callingPackage = extractPackageName(javaClass.getClassName());
		if (calledPackage.length() > 0
				&& callingPackage.length() > 0
				&& (calledPackage.startsWith(callingPackage)
				|| callingPackage.startsWith(calledPackage)))
			priority++;
		// System.out.println("priority: " + priority);

		bugReporter.reportBug(new BugInstance(this, "RV_RETURN_VALUE_IGNORED2",
				priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addCalledMethod(methodGen, inv)
				.addSourceLine(classContext, methodGen, sourceFile, call));
	}

	public static String extractPackageName(String className) {
		int i = className.lastIndexOf('.');
		if (i == -1) return "";
		return className.substring(0, i);
	}

}

// vim:ts=4
