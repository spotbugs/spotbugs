/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005-2006 University of Maryland
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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public @java.lang.Deprecated class FindBadCast extends OpcodeStackDetector implements  StatelessDetector {


	private HashSet<String> castTo = new HashSet<String>();

	BugReporter bugReporter;

	final static boolean DEBUG = false;

	public FindBadCast(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		abstractCollectionClasses.add("java/util/Collection");
		abstractCollectionClasses.add("java/util/List");
		abstractCollectionClasses.add("java/util/Set");
		abstractCollectionClasses.add("java/util/Map");
		concreteCollectionClasses.add("java/util/LinkedHashMap");
		concreteCollectionClasses.add("java/util/LinkedHashSet");
		concreteCollectionClasses.add("java/util/HashMap");
		concreteCollectionClasses.add("java/util/HashSet");
		concreteCollectionClasses.add("java/util/TreeMap");
		concreteCollectionClasses.add("java/util/TreeSet");
		concreteCollectionClasses.add("java/util/ArrayList");
		concreteCollectionClasses.add("java/util/LinkedList");
		concreteCollectionClasses.add("java/util/Hashtable");
		concreteCollectionClasses.add("java/util/Vector");
	}

	private Set<String> concreteCollectionClasses = new HashSet<String>();
	private Set<String> abstractCollectionClasses = new HashSet<String>();




	private int parameters;
	@Override
		 public void visit(Code obj) {
		if (getMethod().isSynthetic()) return;
		if (DEBUG)  {
			System.out.println(getFullyQualifiedMethodName());
			}
		parameters = stack.resetForMethodEntry(this);
		castTo.clear();
		super.visit(obj);
	}


	@Override
		 public void sawOpcode(int seen) {
		if (DEBUG) {
			System.out.println(stack);
			printOpCode(seen);
		}

		if (stack.getStackDepth() > 0) {
			if (seen == CHECKCAST || seen == INSTANCEOF) {
				if (DEBUG)
					System.out.println(" ... checking ... ");
				OpcodeStack.Item it = stack.getStackItem(0);
				String signature = it.getSignature();
				if (signature.length() > 0 && signature.charAt(0) == 'L')
					signature = signature.substring(1, signature.length() - 1);
				String signatureDot = signature.replace('/', '.');
				String to = getClassConstantOperand();
				if (to.length() > 0 && to.charAt(0) == 'L')
					to = to.substring(1, to.length() - 1);
				String toDot = to.replace('/', '.');
				if (signature.length() > 0
						&& !signature.equals("java/lang/Object")
						&& !signature.equals(to)) {

					try {
						JavaClass toClass = Repository.lookupClass(toDot);
						JavaClass signatureClass = Repository
								.lookupClass(signatureDot);
						if (DEBUG)
							System.out.println(" ... checking ...... ");
						if (!castTo.contains(to)
								&& !Repository.instanceOf(signatureClass,
										toClass)) {
							if (!Repository.instanceOf(toClass, signatureClass)
									&& ((!toClass.isInterface() && !signatureClass
											.isInterface())
											|| signatureClass.isFinal() || toClass
											.isFinal()))
								bugReporter
										.reportBug(new BugInstance(
												this,
												seen == CHECKCAST ? "BC_IMPOSSIBLE_CAST"
														: "BC_IMPOSSIBLE_INSTANCEOF",
												seen == CHECKCAST ? HIGH_PRIORITY
														: NORMAL_PRIORITY)
												.addClassAndMethod(this)
												.addSourceLine(this).addClass(
														signatureDot).addClass(
														toDot));
							else if (seen == CHECKCAST) {
								int priority = NORMAL_PRIORITY;
								if (DEBUG) {
									System.out.println("Checking BC in "
											+ getFullyQualifiedMethodName());
									System.out.println("to class: " + toClass);
									System.out.println("from class: "
											+ signatureClass);
									System.out.println("instanceof : "
											+ Repository.instanceOf(toClass,
													signatureClass));
								}
								if (Repository.instanceOf(toClass,
										signatureClass))
									priority += 2;
								if (getThisClass().equals(toClass)
										|| getThisClass()
												.equals(signatureClass))
									priority += 1;
								if (DEBUG)
									System.out
											.println(" priority: " + priority);
								if (toClass.isInterface())
									priority++;
								if (DEBUG)
									System.out
											.println(" priority: " + priority);
								if (priority <= LOW_PRIORITY
										&& (signatureClass.isInterface() || signatureClass
												.isAbstract()))
									priority++;
								if (DEBUG)
									System.out
											.println(" priority: " + priority);
								if (concreteCollectionClasses
										.contains(signature)
										|| abstractCollectionClasses
												.contains(signature))
									priority--;
								if (concreteCollectionClasses.contains(to)
										|| abstractCollectionClasses
												.contains(to))
									priority--;
								if (DEBUG)
									System.out
											.println(" priority: " + priority);
								int reg = it.getRegisterNumber();
								if (reg >= 0 && reg < parameters
										&& it.isInitialParameter()
										&& getMethod().isPublic()) {
									priority--;
									if (getPC() < 4 && priority > LOW_PRIORITY)
										priority--;
								}
								if (DEBUG)
									System.out
											.println(" priority: " + priority);
								if (getMethodName().equals("compareTo"))
									priority++;
								if (DEBUG)
									System.out
											.println(" priority: " + priority);
								if (priority < HIGH_PRIORITY)
									priority = HIGH_PRIORITY;
								if (priority <= LOW_PRIORITY) {
									String bug = "BC_UNCONFIRMED_CAST";
									if (concreteCollectionClasses.contains(to))
										bug = "BC_BAD_CAST_TO_CONCRETE_COLLECTION";
									else if (abstractCollectionClasses
											.contains(to)
											&& (signature
													.equals("java/util/Collection") || signature
													.equals("java/lang/Iterable")))
										bug = "BC_BAD_CAST_TO_ABSTRACT_COLLECTION";
									bugReporter.reportBug(new BugInstance(this,
											bug, priority).addClassAndMethod(
											this).addSourceLine(this).addClass(
											signatureDot).addClass(toDot));
								}
							}

						}

					} catch (RuntimeException e) {
						//		    	 ignore it
					} catch (ClassNotFoundException e) {
						// ignore it
					}
				}
			}
		if (seen == INSTANCEOF) {
			String to = getClassConstantOperand();
			castTo.add(to);
			}
		}

	}




}
