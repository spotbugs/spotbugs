/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.AnalysisLocal;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;

/**
 * Look for calls to methods where the return value is
 * erroneously ignored.  This detector is meant as a simpler
 * and faster replacement for BCPMethodReturnCheck.
 * 
 * @author David Hovemeyer
 */
public class MethodReturnCheck extends BytecodeScanningDetector {
	private static boolean DEBUG = Boolean.getBoolean("mrc.debug");
	private static final boolean CHECK_ALL = Boolean.getBoolean("mrc.checkall");
	
	private static final int SCAN =0;
	private static final int SAW_INVOKE = 1;
	
	private static final BitSet INVOKE_OPCODE_SET = new BitSet();
	static {
		INVOKE_OPCODE_SET.set(Constants.INVOKEINTERFACE);
		INVOKE_OPCODE_SET.set(Constants.INVOKESPECIAL);
		INVOKE_OPCODE_SET.set(Constants.INVOKESTATIC);
		INVOKE_OPCODE_SET.set(Constants.INVOKEVIRTUAL);
	}
	
	private static final String ANY = null;
	
	// Methods to look for, as tuples "class, method, signature".
	// Null means "any".  In method and signature, the string
	// is treated as a pattern, where the * can match
	// any sequence of characters, and all other characters are
	// matched literally.
	private static final String[][] STANDARD_POLICY_DATABASE = {
		// Class                        Method              Signature
		{ANY,                           "equals",           "(Ljava/lang/Object;)Z"},
		{"java.lang.String",            ANY,                "*)Ljava/lang/String;"},
		{"java.lang.StringBuffer",      "toString",         "*)Ljava/lang/String;"},
		{"java.lang.Thread",            "<init>",           ANY},
		{"java.lang.Throwable",         "<init>",           ANY},
		{"java.security.MessageDigest", "digest",           "([B)[B"},
		{"java.sql.Connection",         ANY,                ANY},
		{"java.net.InetAddress",        ANY,                ANY},
		{"java.math.BigDecimal",        ANY,                ANY},
		{"java.math.BigInteger",        ANY,                ANY},
		{"java.util.Enumeration",       "hasMoreElements",  "()Z"},
		{"java.util.Iterator",          "hasNext",          "()Z"},
		{"java.io.File",                "createNewFile",    "()Z"},
//		{"",                            "",                 ""},
	};
	private static final String[][] JDK15_POLICY_DATABASE = {
		// Class                                    Method        Signature
		{"java.util.concurrent.locks.ReadWriteLock","readLock",   "()Ljava/util/concurrent/locks/Lock;"},
		{"java.util.concurrent.locks.ReadWriteLock","writeLock",  "()Ljava/util/concurrent/locks/Lock;"},
		{"java.util.concurrent.locks.Condition",    "await",      "(JLjava/util/concurrent/TimeUnit;)Z"},
		{"java.util.concurrent.locks.Condition",    "awaitUtil",  "(Ljava/util/Date;)Z"},
		{"java.util.concurrent.locks.Condition",    "awaitNanos", "(J)Z"},
		{"java.util.concurrent.Semaphore",          "tryAcquire", "(JLjava/util/concurrent/TimeUnit;)Z"},
		{"java.util.concurrent.Semaphore",          "tryAcquire", "()Z"},
		{"java.util.concurrent.locks.Lock",         "tryLock",    "(JLjava/util/concurrent/TimeUnit;)Z"},
		{"java.util.concurrent.locks.Lock",         "newCondition","()Ljava/util/concurrent/locks/Condition;"},
		{"java.util.concurrent.locks.Lock",         "tryLock",     "()Z"},
		{"java.util.Queue",                         "offer",       "(Ljava/lang/Object;)Z"},
		{"java.util.concurrent.BlockingQueue",      "offer",       "(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z"},
		{"java.util.concurrent.BlockingQueue",      "poll",        "(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;"},
		{"java.util.Queue",                         "poll",        "()Ljava/lang/Object;"},
//		{"",                                        "",            ""},
	};
	
	private static class PolicyDatabaseEntry {
		private String className, methodName, signature;
		private Pattern methodPattern, signaturePattern;
		
		public PolicyDatabaseEntry(String className, String methodName, String signature) {
			this.className = className;
			this.methodName = methodName;
			this.signature = signature;
			this.methodPattern = createPattern(methodName);
			this.signaturePattern = createPattern(signature);
		}
		
		public boolean match(String className, String methodName, String signature) throws ClassNotFoundException {
			return matchClass(className)
				&& matchElement(methodName, this.methodName, this.methodPattern)
				&& matchElement(signature, this.signature, this.signaturePattern);
		}

		private boolean matchClass(String className) throws ClassNotFoundException {
			return this.className == null
				|| Hierarchy.isSubtype(className, this.className);
		}

		private boolean matchElement(String value, String expected, Pattern pattern) {
			if (expected == null) {
				return true;
			} else if (pattern == null) {
				return value.equals(expected); 
			} else {
				Matcher matcher = pattern.matcher(value);
				return matcher.matches();
			}
		}

		private Pattern createPattern(String s) {
			if (s == null || s.indexOf('*') < 0)
				return null;

			StringBuffer regex = new StringBuffer();
			regex.append('^');
			while (s.length() > 0) {
				int star = s.indexOf('*');
				if (star < 0) {
					appendLiteral(regex, s);
					s = "";
				} else {
					appendLiteral(regex, s.substring(0, star));
					regex.append(".*");
					s = s.substring(star+1);
				}
			}
			regex.append('$');
			
			return Pattern.compile(regex.toString());
		}

		private void appendLiteral(StringBuffer regex, String s) {
			regex.append("\\Q");
			regex.append(s);
			regex.append("\\E");
		}
	}

	private static class PolicyDatabase {
		private List<PolicyDatabaseEntry> database;
		
		public PolicyDatabase() {
			this.database = new ArrayList<PolicyDatabaseEntry>();
		}
		
		public void add(PolicyDatabaseEntry entry) {
			database.add(entry);
		}
		
		public boolean match(String className, String methodName, String signature)
				throws ClassNotFoundException {
			for (Iterator<PolicyDatabaseEntry> i = database.iterator(); i.hasNext(); ) {
				PolicyDatabaseEntry entry = i.next();
				if (entry.match(className, methodName, signature))
					return true;
			}
			return false;
		}
	}
	
	static AnalysisLocal<PolicyDatabase> policyDatabaseLocal =
		new AnalysisLocal<PolicyDatabase>();

	static PolicyDatabase getDatabase() {
		PolicyDatabase database = policyDatabaseLocal.get();
		if (database == null) {
			database = new PolicyDatabase();
			for (int i = 0; i < STANDARD_POLICY_DATABASE.length; ++i) {
				String[] tuple = STANDARD_POLICY_DATABASE[i];
				database.add(new PolicyDatabaseEntry(tuple[0], tuple[1], tuple[2]));
			}
			
			// TODO: should add policies set by @CheckReturnValue annotations
			
			policyDatabaseLocal.set(database);
		}
		return database;
	}
		
	private BugReporter bugReporter;
	private ClassContext classContext;
	private Method method;
	private int state;
	private int callPC;
	private String className, methodName, signature;
	
	public MethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		super.visitClassContext(classContext);
		this.classContext = null;
	}
	
	public void visit(Method method) {
		this.method = method;
	}
	
	public void visitCode(Code code) {
		// Prescreen to find methods with POP or POP2 instructions,
		// and at least one method invocation
		if (!prescreen())
			return;
		
		if (DEBUG) System.out.println("Visiting " + method);
		super.visitCode(code);
	}

	private boolean prescreen() {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (!(bytecodeSet.get(Constants.POP) || bytecodeSet.get(Constants.POP2))) {
			return false;
		} else if (!(bytecodeSet.get(Constants.INVOKEINTERFACE) ||
				bytecodeSet.get(Constants.INVOKESPECIAL) ||
				bytecodeSet.get(Constants.INVOKESTATIC) ||
				bytecodeSet.get(Constants.INVOKEVIRTUAL))) {
			return false;
		}
		return true;
	}
	
	public void sawOpcode(int seen) {
		boolean redo;
		
		do {
			redo = false;
			switch (state) {
			case SCAN:
				if (INVOKE_OPCODE_SET.get(seen)) {
					callPC = getPC();
					className = getDottedClassConstantOperand();
					methodName = getNameConstantOperand();
					signature = getSigConstantOperand();
					if (requiresReturnValueCheck()) {
						if (DEBUG) System.out.println(
								"Saw "+className+"."+methodName+":"+signature+" @"+callPC);
						state = SAW_INVOKE;
					}
				}
				break;
				
			case SAW_INVOKE:
				if (isPop(seen)) {
					int popPC = getPC();
					if (DEBUG) System.out.println("Saw POP @"+popPC);
					BugInstance warning =
						new BugInstance(this, "RV_RETURN_VALUE_IGNORED", NORMAL_PRIORITY)
							.addClassAndMethod(this)
							.addMethod(className, methodName, signature, seen == Constants.INVOKESTATIC).describe("METHOD_CALLED")
							.addSourceLine(this, callPC);
					bugReporter.reportBug(warning);
				} else {
					// This instruction might be an invocation, too.
					// So redo processing this instruction.
					redo = true;
				}
				state = SCAN;
				break;
				
			default:
			}
		} while (redo);

	}

	private boolean isPop(int seen) {
		return seen == Constants.POP || seen == Constants.POP2;
	}

	private boolean requiresReturnValueCheck() {
		if (DEBUG) {
			System.out.println("Trying: "+className+"."+methodName+":"+signature);
		}
		try {
			return getDatabase().match(className, methodName, signature);
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return false;
		}
	}
}
