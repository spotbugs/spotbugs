/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.meta.When;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.jsr305.Analysis;
import edu.umd.cs.findbugs.ba.jsr305.BackwardTypeQualifierDataflow;
import edu.umd.cs.findbugs.ba.jsr305.BackwardTypeQualifierDataflowAnalysis;
import edu.umd.cs.findbugs.ba.jsr305.BackwardTypeQualifierDataflowFactory;
import edu.umd.cs.findbugs.ba.jsr305.FlowValue;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflow;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflowAnalysis;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflowFactory;
import edu.umd.cs.findbugs.ba.jsr305.SourceSinkInfo;
import edu.umd.cs.findbugs.ba.jsr305.SourceSinkType;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValueSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.MissingClassException;

/**
 * Check JSR-305 type qualifiers.
 * 
 * @author David Hovemeyer
 */
public class CheckTypeQualifiers extends CFGDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("ctq.debug");
	private static final boolean DEBUG_DATAFLOW = SystemProperties.getBoolean("ctq.dataflow.debug");
	private static final String DEBUG_DATAFLOW_MODE = SystemProperties.getProperty("ctq.dataflow.debug.mode", "both");

	private final BugReporter bugReporter;
	private boolean firstTime;

	public CheckTypeQualifiers(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.firstTime = true;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.bcel.CFGDetector#visitClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	@Override
	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		
		super.visitClass(classDescriptor);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.bcel.CFGDetector#visitMethodCFG(edu.umd.cs.findbugs.classfile.MethodDescriptor, edu.umd.cs.findbugs.ba.CFG)
	 */
	@Override
	protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {
		if (DEBUG) {
			System.out.println("CheckTypeQualifiers: checking " + methodDescriptor.toString());
		}

		IAnalysisCache analysisCache = Global.getAnalysisCache();
		ForwardTypeQualifierDataflowFactory forwardDataflowFactory =
			analysisCache.getMethodAnalysis(ForwardTypeQualifierDataflowFactory.class, methodDescriptor);
		BackwardTypeQualifierDataflowFactory backwardDataflowFactory =
			analysisCache.getMethodAnalysis(BackwardTypeQualifierDataflowFactory.class, methodDescriptor);
		ValueNumberDataflow vnaDataflow =
			analysisCache.getMethodAnalysis(ValueNumberDataflow.class, methodDescriptor);

		Collection<TypeQualifierValue> relevantQualifiers = Analysis.getRelevantTypeQualifiers(methodDescriptor);
		if (DEBUG) {
			System.out.println("  Relevant type qualifiers are " + relevantQualifiers);
		}

		for (TypeQualifierValue typeQualifierValue : relevantQualifiers) {
			if (typeQualifierValue.getTypeQualifierClassDescriptor().getClassName().equals("javax/annotation/Nonnull")) {
				// Checking @Nonnull annotations is the bailiwick of FindNullDeref.
				continue;
			}
			
			try {
				checkQualifier(
						methodDescriptor,
						cfg,
						typeQualifierValue,
						forwardDataflowFactory,
						backwardDataflowFactory,
						vnaDataflow
				);
			} catch (MissingClassException e) {
				bugReporter.reportMissingClass(e.getClassDescriptor());
			} catch (CheckedAnalysisException e) {
				bugReporter.logError(
						"Exception checking type qualifier " + typeQualifierValue.toString() +
						" on method " + methodDescriptor.toString(),
						e);
			}
		}
	}

	private String checkLocation;

	/**
	 * Check a specific TypeQualifierValue on a method.
	 * 
	 * @param methodDescriptor       MethodDescriptor of method
	 * @param cfg                    CFG of method
	 * @param typeQualifierValue     TypeQualifierValue to check
	 * @param forwardDataflowFactory ForwardTypeQualifierDataflowFactory used to create forward dataflow analysis objects
	 * @param backwardDataflowFactory BackwardTypeQualifierDataflowFactory used to create backward dataflow analysis objects
	 * @param vnaDataflow            ValueNumberDataflow for the method
	 */
	private void checkQualifier(
			MethodDescriptor methodDescriptor,
			CFG cfg,
			TypeQualifierValue typeQualifierValue,
			ForwardTypeQualifierDataflowFactory forwardDataflowFactory,
			BackwardTypeQualifierDataflowFactory backwardDataflowFactory,
			ValueNumberDataflow vnaDataflow) throws CheckedAnalysisException {

		if (DEBUG) {
			System.out.println("----------------------------------------------------------------------");
			System.out.println("Checking type qualifier " + typeQualifierValue.toString() + " on method " + methodDescriptor.toString());
			if (typeQualifierValue.isStrictQualifier()) {
				System.out.println("  Strict type qualifier");
			}
			System.out.println("----------------------------------------------------------------------");
		}

		ForwardTypeQualifierDataflow forwardDataflow = forwardDataflowFactory.getDataflow(typeQualifierValue);

		if (DEBUG_DATAFLOW && (DEBUG_DATAFLOW_MODE.startsWith("forward") || DEBUG_DATAFLOW_MODE.equals("both"))) {
			System.out.println("********* Forwards analysis *********");
			DataflowCFGPrinter<TypeQualifierValueSet, ForwardTypeQualifierDataflowAnalysis> p =
				new DataflowCFGPrinter<TypeQualifierValueSet, ForwardTypeQualifierDataflowAnalysis>(forwardDataflow);
			p.print(System.out);
		}

		BackwardTypeQualifierDataflow backwardDataflow = backwardDataflowFactory.getDataflow(typeQualifierValue);

		if (DEBUG_DATAFLOW && (DEBUG_DATAFLOW_MODE.startsWith("backward") || DEBUG_DATAFLOW_MODE.equals("both"))) {
			System.out.println("********* Backwards analysis *********");
			DataflowCFGPrinter<TypeQualifierValueSet, BackwardTypeQualifierDataflowAnalysis> p =
				new DataflowCFGPrinter<TypeQualifierValueSet, BackwardTypeQualifierDataflowAnalysis>(backwardDataflow);
			p.print(System.out);
		}

		checkDataflow(methodDescriptor, cfg, typeQualifierValue, vnaDataflow, forwardDataflow, backwardDataflow);
		checkValueSources(methodDescriptor, cfg, typeQualifierValue, vnaDataflow, forwardDataflow, backwardDataflow);
	}

	private void checkDataflow(MethodDescriptor methodDescriptor, CFG cfg, TypeQualifierValue typeQualifierValue,
			ValueNumberDataflow vnaDataflow, ForwardTypeQualifierDataflow forwardDataflow,
			BackwardTypeQualifierDataflow backwardDataflow) throws DataflowAnalysisException, CheckedAnalysisException {
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location loc = i.next();

			TypeQualifierValueSet forwardsFact = forwardDataflow.getFactAtLocation(loc);
			TypeQualifierValueSet backwardsFact = backwardDataflow.getFactAfterLocation(loc);

			if (!forwardsFact.isValid() || !backwardsFact.isValid()) {
				continue;
			}

			if (DEBUG) {
				checkLocation = "location " + loc.toCompactString();
			}
			checkForConflictingValues(
					methodDescriptor,
					typeQualifierValue,
					forwardsFact,
					backwardsFact,
					loc, // location to report
					loc, // location where doomed value is observed
					vnaDataflow.getFactAtLocation(loc)
			);
		}

		for (Iterator<Edge> i = cfg.edgeIterator(); i.hasNext(); ) {
			Edge edge = i.next();
			if (DEBUG) {
				checkLocation = "edge " + edge.getLabel();
				System.out.println("BEGIN CHECK EDGE " + edge.getLabel());
			}

			// NOTE: when checking forwards and backwards values on an edge,
			// we don't want to apply BOTH edge transfer functions,
			// since the purpose of the edge transfer function is to
			// propagate information across phi nodes (effectively
			// copying information about one value to another).
			// Due to pruning of backwards values when a conflict is detected,
			// we need to check backwards values as "early" as possible,
			// meaning that we want to check at the edge target
			// (before the backwards edge transfer function has pruned
			// the backwards value.)
			TypeQualifierValueSet forwardFact = forwardDataflow.getFactOnEdge(edge);
			TypeQualifierValueSet backwardFact = backwardDataflow.getResultFact(edge.getTarget());

			// The edge target location is where we can check
			// for conflicting flow values.
			Location edgeTargetLocation = getEdgeTargetLocation(cfg, edge);
			ValueNumberFrame vnaFrame = (edgeTargetLocation != null) ? vnaDataflow.getFactAtLocation(edgeTargetLocation) : null; 

			// What location do we want to report to the user
			// as where the conflict occurs?
			// The edge source location is generally better,
			// but edge target location is ok as a fallback.
			Location locationToReport;
			if (edge.getSource().getLastInstruction() != null) {
				locationToReport = getEdgeSourceLocation(cfg, edge);
			} else {
				locationToReport = edgeTargetLocation;
			}

			checkForConflictingValues(
					methodDescriptor,
					typeQualifierValue,
					forwardFact,
					backwardFact,
					locationToReport,
					edgeTargetLocation,
					vnaFrame
			);
			if (DEBUG) {
				System.out.println("END CHECK EDGE");
			}
		}
	}

	private void checkValueSources(MethodDescriptor methodDescriptor, CFG cfg, TypeQualifierValue typeQualifierValue,
			ValueNumberDataflow vnaDataflow, ForwardTypeQualifierDataflow forwardDataflow,
			BackwardTypeQualifierDataflow backwardDataflow) throws DataflowAnalysisException {

		// Check to see if any backwards ALWAYS or NEVER values
		// reach incompatible sources.

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();

			Set<SourceSinkInfo> sourceSet = forwardDataflow.getAnalysis().getSourceSinkInfoSet(location);

			for (SourceSinkInfo source : sourceSet) {
				ValueNumber vn = source.getValueNumber();
				TypeQualifierValueSet backwardsFact = backwardDataflow.getFactAtLocation(location);
				FlowValue backwardsFlowValue = backwardsFact.getValue(vn);

				if (DEBUG) {
					System.out.println("Checking value source at " + location.toCompactString());
					System.out.println("  back=" + backwardsFact);
					System.out.println("  source=" + source);
				}

				if (!(backwardsFlowValue == FlowValue.ALWAYS || backwardsFlowValue == FlowValue.NEVER)) {
					continue;
				}

				// Check to see if this warning has already been reported because
				// the dataflow values conflict directly with each other.
				TypeQualifierValueSet forwardsFact = forwardDataflow.getFactAfterLocation(location);
				FlowValue forwardsFlowValue = forwardsFact.getValue(vn);
				if (FlowValue.valuesConflict(forwardsFlowValue, backwardsFlowValue)) {
					continue;
				}

				if (FlowValue.backwardsValueConflictsWithSource(backwardsFlowValue, source, typeQualifierValue)) {
					String bugType =
						(backwardsFlowValue == FlowValue.NEVER) ? "TQ_MAYBE_SOURCE_VALUE_REACHES_NEVER_SINK" : "TQ_MAYBE_SOURCE_VALUE_REACHES_ALWAYS_SINK";

					emitSourceWarning(
							bugType,
							methodDescriptor,
							typeQualifierValue,
							backwardsFlowValue,
							backwardsFact,
							source,
							vn, location
					);
				} else if (source.getWhen() == When.UNKNOWN && source.getType() == SourceSinkType.PARAMETER) {

					XMethod xmethod = XFactory.createXMethod(methodDescriptor);
					int p = source.getParameter();
					TypeQualifierAnnotation directTypeQualifierAnnotation = TypeQualifierApplications.getDirectTypeQualifierAnnotation(xmethod, p, typeQualifierValue);
					if (directTypeQualifierAnnotation != null && directTypeQualifierAnnotation.when == When.UNKNOWN) {
						String bugType =
							(backwardsFlowValue == FlowValue.NEVER) ? "TQ_EXPLICIT_UNKNOWN_SOURCE_VALUE_REACHES_NEVER_SINK" : "TQ_EXPLICIT_UNKNOWN_SOURCE_VALUE_REACHES_ALWAYS_SINK";


						emitSourceWarning(
								bugType,
								methodDescriptor,
								typeQualifierValue,
								backwardsFlowValue,
								backwardsFact,
								source,
								vn, location
						);
					}

				}}
		}

	}


	private Location getEdgeTargetLocation(CFG cfg, Edge edge) {
		BasicBlock targetBlock = edge.getTarget();

		// Target block is nonempty?
		if (targetBlock.getFirstInstruction() != null) {
			return new Location(targetBlock.getFirstInstruction(), targetBlock);
		}

		// Target block is an ETB?
		if (targetBlock.isExceptionThrower()) {
			BasicBlock fallThroughSuccessor = cfg.getSuccessorWithEdgeType(targetBlock, EdgeTypes.FALL_THROUGH_EDGE);
			if (fallThroughSuccessor == null) {
				// Fall through edge might have been pruned
				for (Iterator<Edge> i = cfg.removedEdgeIterator(); i.hasNext(); ) {
					Edge removedEdge = i.next();
					if (removedEdge.getSource() == targetBlock && removedEdge.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
						fallThroughSuccessor = removedEdge.getTarget();
						break;
					}
				}
			}

			if (fallThroughSuccessor != null && fallThroughSuccessor.getFirstInstruction() != null) {
				return new Location(fallThroughSuccessor.getFirstInstruction(), fallThroughSuccessor);
			}
		}

		return null;
	}

	private Location getEdgeSourceLocation(CFG cfg, Edge edge) {
		BasicBlock sourceBlock = edge.getSource();
		return (sourceBlock.getLastInstruction() != null) ? new Location(sourceBlock.getLastInstruction(), sourceBlock) : null;
	}

	private void checkForConflictingValues(
			MethodDescriptor methodDescriptor,
			TypeQualifierValue typeQualifierValue,
			TypeQualifierValueSet forwardsFact,
			TypeQualifierValueSet backwardsFact,
			Location locationToReport,
			Location locationWhereDoomedValueIsObserved,
			ValueNumberFrame vnaFrame) throws CheckedAnalysisException {
		Set<ValueNumber> valueNumberSet = new HashSet<ValueNumber>();
		valueNumberSet.addAll(forwardsFact.getValueNumbers());
		valueNumberSet.addAll(backwardsFact.getValueNumbers());

		for (ValueNumber vn : valueNumberSet) {
			FlowValue forward = forwardsFact.getValue(vn);
			FlowValue backward = backwardsFact.getValue(vn);

			if (DEBUG) {
				System.out.println("Check " + vn + ": forward=" + forward + ", backward=" + backward + " at " + checkLocation);
			}

			if (FlowValue.valuesConflict(forward, backward)) {
				if (DEBUG) {
					System.out.println("Emitting warning at " + checkLocation);
				}
				emitDataflowWarning(
						methodDescriptor,
						typeQualifierValue,
						forwardsFact,
						backwardsFact,
						vn,
						forward,
						backward,
						locationToReport,
						locationWhereDoomedValueIsObserved,
						vnaFrame);
			}
		}
	}

	private void emitDataflowWarning(
			MethodDescriptor methodDescriptor,
			TypeQualifierValue typeQualifierValue,
			TypeQualifierValueSet forwardsFact,
			TypeQualifierValueSet backwardsFact,
			ValueNumber vn,
			FlowValue forward,
			FlowValue backward,
			Location locationToReport,
			Location locationWhereDoomedValueIsObserved,
			ValueNumberFrame vnaFrame) throws CheckedAnalysisException {
		String bugType =
			(backward == FlowValue.NEVER) ? "TQ_ALWAYS_VALUE_USED_WHERE_NEVER_REQUIRED" : "TQ_NEVER_VALUE_USED_WHERE_ALWAYS_REQUIRED";

		// Issue warning
		BugInstance warning = new BugInstance(this, bugType, Priorities.NORMAL_PRIORITY)
			.addClassAndMethod(methodDescriptor);
		annotateWarningWithTypeQualifier(warning, typeQualifierValue);

		// Hopefully we can find the conflicted value in a local variable
		if (locationWhereDoomedValueIsObserved != null) {
			Method method = Global.getAnalysisCache().getMethodAnalysis(Method.class, methodDescriptor); 
			LocalVariableAnnotation localVariable =
				ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, locationWhereDoomedValueIsObserved, vn, vnaFrame);
			if (localVariable != null) {
				localVariable.setDescription(localVariable.isSignificant() ? "LOCAL_VARIABLE_VALUE_DOOMED_NAMED" : "LOCAL_VARIABLE_VALUE_DOOMED");
				warning.add(localVariable);
			}
			// Report where we observed the value.
			// Note that for conflicts detected on control edges,
			// we REPORT the edge source location
			// rather than the target location, even though it is the
			// target location where the conflict is detected.
			// The only reason to use a different reporting location
			// is to produce a more informative report for the user,
			// since the edge source is where the branch is found.
			SourceLineAnnotation observedLocation = SourceLineAnnotation.fromVisitedInstruction(methodDescriptor, locationToReport);
			observedLocation.setDescription("SOURCE_LINE_VALUE_DOOMED");
			warning.add(observedLocation);
		}

		/*
		// Add value sources
		Set<SourceSinkInfo> sourceSet = (forward == FlowValue.ALWAYS) ? forwardsFact.getWhereAlways(vn) : forwardsFact.getWhereNever(vn);
		for (SourceSinkInfo source : sourceSet) {
			annotateWarningWithSourceSinkInfo(warning, methodDescriptor, vn, source);
		}
		*/

		// Add value sinks
		Set<SourceSinkInfo> sinkSet = (backward == FlowValue.ALWAYS) ? backwardsFact.getWhereAlways(vn) : backwardsFact.getWhereNever(vn);
		for (SourceSinkInfo sink : sinkSet) {
			annotateWarningWithSourceSinkInfo(warning, methodDescriptor, vn, sink);
		}

		bugReporter.reportBug(warning);
	}

	private void emitSourceWarning(
			String bugType,
			MethodDescriptor methodDescriptor,
			TypeQualifierValue typeQualifierValue,
			FlowValue backwardsFlowValue,
			TypeQualifierValueSet backwardsFact,
			SourceSinkInfo source,
			ValueNumber vn, Location location) {
		
		BugInstance warning = new BugInstance(this, bugType, Priorities.NORMAL_PRIORITY)
			.addClassAndMethod(methodDescriptor);
		annotateWarningWithTypeQualifier(warning, typeQualifierValue);
		
		annotateWarningWithSourceSinkInfo(warning, methodDescriptor, vn, source);
		
		Set<SourceSinkInfo> sinkSet = (backwardsFlowValue == FlowValue.NEVER) ? backwardsFact.getWhereNever(vn) : backwardsFact.getWhereAlways(vn);
		for (SourceSinkInfo sink : sinkSet) {
			annotateWarningWithSourceSinkInfo(warning, methodDescriptor, vn, sink);
		}
		
		bugReporter.reportBug(warning);
	}

	private void annotateWarningWithTypeQualifier(BugInstance warning, TypeQualifierValue typeQualifierValue) {
		StringBuilder buf = new StringBuilder();
		buf.append("@");
		buf.append(typeQualifierValue.typeQualifier.getDottedClassName());
		if (TypeQualifierValue.hasMultipleVariants(typeQualifierValue)) {
			// When there are multiple variants, qualify the type
			// qualifier with the value indicating which variant.
			buf.append("(");
			buf.append(typeQualifierValue.value);
			buf.append(")");
		}
		warning.addString(buf.toString()).describe(StringAnnotation.TYPE_QUALIFIER_ROLE);
	}

	private void annotateWarningWithSourceSinkInfo(BugInstance warning, MethodDescriptor methodDescriptor, ValueNumber vn, SourceSinkInfo sourceSinkInfo) {
		switch (sourceSinkInfo.getType()) {
		case PARAMETER:
			try {
				Method method = Global.getAnalysisCache().getMethodAnalysis(Method.class, methodDescriptor);
				LocalVariableAnnotation lva = LocalVariableAnnotation.getParameterLocalVariableAnnotation(
						method,
						sourceSinkInfo.getLocal());
				lva.setDescription(lva.isSignificant()
						? "LOCAL_VARIABLE_PARAMETER_VALUE_SOURCE_NAMED" : "LOCAL_VARIABLE_PARAMETER_VALUE_SOURCE");
				warning.add(lva);
			} catch (CheckedAnalysisException e) {
				warning.addSourceLine(methodDescriptor, sourceSinkInfo.getLocation()).describe("SOURCE_LINE_VALUE_SOURCE");
			}
			break;

		case RETURN_VALUE_OF_CALLED_METHOD:
		case FIELD_LOAD:
			warning.addSourceLine(methodDescriptor, sourceSinkInfo.getLocation()).describe("SOURCE_LINE_VALUE_SOURCE");
			break;

		case ARGUMENT_TO_CALLED_METHOD:
		case RETURN_VALUE:
		case FIELD_STORE:
			warning.addSourceLine(methodDescriptor, sourceSinkInfo.getLocation()).describe("SOURCE_LINE_VALUE_SINK");
			return;

		default:
			throw new IllegalStateException();
		}
	}
}
