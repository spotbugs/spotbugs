/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
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
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.ByteSequence;

/** @author Dave Brousius  4/2005  original author
 *  @author Brian Cole     7/2006  serious reworking
 */
public class DuplicateBranches extends PreorderVisitor implements Detector
{
	private ClassContext classContext;
	private BugReporter bugReporter;

	public DuplicateBranches(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		classContext.getJavaClass().accept(this);
	}

	@Override
		 public void visitMethod(Method method) {
		try {
			if (method.getCode() == null)
				return;

			CFG cfg = classContext.getCFG(method);

			Iterator<BasicBlock> bbi = cfg.blockIterator();
			while (bbi.hasNext()) {
				BasicBlock bb = bbi.next();

				int numOutgoing = cfg.getNumOutgoingEdges(bb);
				if (numOutgoing == 2)
					findIfElseDuplicates(cfg, method, bb);
				else if (numOutgoing > 2)
					findSwitchDuplicates(cfg, method, bb);
			}
		} catch (MethodUnprofitableException mue) {
			if (SystemProperties.getBoolean("unprofitable.debug")) // otherwise don't report
				bugReporter.logError("skipping unprofitable method in " + getClass().getName());
		} catch (Exception e) {
			bugReporter.logError("Failure examining basic blocks in Duplicate Branches detector", e);
		}
	}

	private void findIfElseDuplicates(CFG cfg, Method method, BasicBlock bb) {
		BasicBlock thenBB = null, elseBB = null;

		Iterator<Edge> iei = cfg.outgoingEdgeIterator(bb);
		while (iei.hasNext()) {
			Edge e = iei.next();
			if (e.getType() == EdgeTypes.IFCMP_EDGE) {
				elseBB = e.getTarget();
			}
			else if (e.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
				thenBB = e.getTarget();
			}
		}

		if ((thenBB == null) || (elseBB == null))
			return;
		InstructionHandle thenStartHandle = getDeepFirstInstruction(cfg, thenBB);
		InstructionHandle elseStartHandle = getDeepFirstInstruction(cfg, elseBB);
		if ((thenStartHandle == null) || (elseStartHandle == null))
			return;

		int thenStartPos = thenStartHandle.getPosition();
		int elseStartPos = elseStartHandle.getPosition();

		InstructionHandle thenFinishIns = findThenFinish(cfg, thenBB, elseStartPos);
		int thenFinishPos = thenFinishIns.getPosition();

		if (!(thenFinishIns.getInstruction() instanceof GotoInstruction))
			return;

		InstructionHandle elseFinishHandle =
				((GotoInstruction) thenFinishIns.getInstruction()).getTarget();
		int elseFinishPos = elseFinishHandle.getPosition();

		if (thenFinishPos >= elseStartPos)
			return;

		if ((thenFinishPos - thenStartPos) != (elseFinishPos - elseStartPos))
			return;

		byte[] thenBytes = getCodeBytes(method, thenStartPos, thenFinishPos);
		byte[] elseBytes = getCodeBytes(method, elseStartPos, elseFinishPos);

		if (!Arrays.equals(thenBytes, elseBytes))
			return;

		// adjust elseFinishPos to be inclusive (for source line attribution)
		InstructionHandle elseLastIns = elseFinishHandle.getPrev();
		if (elseLastIns != null) elseFinishPos = elseLastIns.getPosition();

		bugReporter.reportBug(new BugInstance(this, "DB_DUPLICATE_BRANCHES", NORMAL_PRIORITY)
				.addClass(classContext.getJavaClass())
				.addMethod(classContext.getJavaClass(), method)
				.addSourceLineRange(classContext, this, thenStartPos, thenFinishPos)
				.addSourceLineRange(classContext, this, elseStartPos, elseFinishPos));
	}

	/** Like bb.getFirstInstruction() except that if null is
	 *  returned it will follow the FALL_THROUGH_EDGE (if any) */
	private static InstructionHandle getDeepFirstInstruction(CFG cfg, BasicBlock bb) {
		InstructionHandle ih = bb.getFirstInstruction();
		if (ih != null) return ih;
		Iterator<Edge> iei = cfg.outgoingEdgeIterator(bb);
		while (iei.hasNext()) {
			Edge e = iei.next();
			String edgeString = e.toString();
			if (EdgeTypes.FALL_THROUGH_EDGE == e.getType())
				return getDeepFirstInstruction(cfg, e.getTarget());
		}
		return null;
	}

	private void findSwitchDuplicates(CFG cfg, Method method, BasicBlock bb) {		

		int[] switchPos = new int[cfg.getNumOutgoingEdges(bb)+1];
		HashMap<Integer, InstructionHandle> prevHandle = new HashMap<Integer, InstructionHandle>();

		Iterator<Edge> iei = cfg.outgoingEdgeIterator(bb);
		int idx = 0;

		while (iei.hasNext()) {
			Edge e = iei.next();
			int eType = e.getType();
			if (eType == EdgeTypes.SWITCH_EDGE || eType == EdgeTypes.SWITCH_DEFAULT_EDGE) {
				BasicBlock target = e.getTarget();
				InstructionHandle firstIns = getDeepFirstInstruction(cfg, target);
				if (firstIns == null)
					continue; // give up on this edge
				int firstInsPosition = firstIns.getPosition();
				switchPos[idx++] = firstInsPosition;
				InstructionHandle prevIns = firstIns.getPrev(); // prev in bytecode, not flow
				if (prevIns != null) prevHandle.put((Integer)firstInsPosition, prevIns);
			} else {
				// hmm, this must not be a switch statement, so give up
				return;
			}
		}

		if (idx < 2) // need at least two edges to tango
			return;

		Arrays.sort(switchPos, 0, idx); // sort the 'idx' switch positions

		// compute end position of final case (ok if set to 0 or <= switchPos[idx-1])
		switchPos[idx] = getFinalTarget(cfg, switchPos[idx-1], prevHandle.values());

		HashMap<BigInteger, Collection<Integer>> map = new HashMap<BigInteger,Collection<Integer>>();
		for (int i = 0; i < idx; i++) {
			if (switchPos[i]+1 >= switchPos[i+1]) continue; // why the +1 on lhs?

			int endPos = switchPos[i+1];
		   InstructionHandle last = prevHandle.get((Integer)switchPos[i+1]);
			if (last == null) {
				// should be default case -- leave endPos as is
			} else if (last.getInstruction() instanceof GotoInstruction) {
				endPos = last.getPosition(); // don't store the goto
			} else if (last.getInstruction() instanceof ReturnInstruction) {
				// leave endPos as is (store the return instruction)
		//	} else if (last.getInstruction() instanceof ATHROW) {
		//		// leave endPos as is (store the throw instruction)
		// Don't do this since many cases may throw "not implemented".
			} else {
				if (i+2 < idx) continue; // falls through to next case, so don't store it at all
				if (i+1 < idx && switchPos[idx]!=switchPos[idx-1]) continue; // also falls through unless switch has no default case
			}

			BigInteger clauseAsInt = getCodeBytesAsBigInt(method, switchPos, i, endPos);
			updateMap(map, i, clauseAsInt);


		}
		for(Collection<Integer> clauses : map.values()) {
			if (clauses.size() > 1) {
				BugInstance bug = new BugInstance(this, "DB_DUPLICATE_SWITCH_CLAUSES", LOW_PRIORITY)
						.addClass(classContext.getJavaClass())
						.addMethod(classContext.getJavaClass(), method);
				for(int i : clauses) 
					bug.addSourceLineRange(this.classContext, this, 
							switchPos[i],
							switchPos[i+1]-1); // not endPos, but that's ok
				bugReporter.reportBug(bug);
			}
		}
	}


	private void updateMap(HashMap<BigInteger, Collection<Integer>> map, int i, BigInteger clauseAsInt) {
		Collection<Integer> values = map.get(clauseAsInt);

		if (values == null) {
			values = new LinkedList<Integer>();
			map.put(clauseAsInt,values);
		}
		values.add((Integer)i); // index into the sorted array
	}


	private BigInteger getCodeBytesAsBigInt(Method method, int[] switchPos, int i, int endPos) {
		byte[] clause = getCodeBytes(method, switchPos[i], endPos);

		BigInteger clauseAsInt;
		if (clause.length == 0) clauseAsInt = BigInteger.ZERO;
		else clauseAsInt = new BigInteger(clause);
		return clauseAsInt;
	}

	/** determine the end position (exclusive) of the final case
	 *  by looking at the gotos at the ends of the other cases */
	private static int getFinalTarget(CFG cfg, int myPos, Collection<InstructionHandle> prevs) {
		int maxGoto = 0;
		BasicBlock myBB = null;
		// note: InstructionHandle doesn't override equals(), so use prevs.contains() with caution.
		Iterator<BasicBlock> bbi = cfg.blockIterator();
		while (bbi.hasNext()) {
			BasicBlock bb = bbi.next();
			InstructionHandle last = bb.getLastInstruction(); // may be null
			if (prevs.contains(last)) { // danger will robinson
				Iterator<Edge> iei = cfg.outgoingEdgeIterator(bb);
				while (iei.hasNext()) {
					Edge e = iei.next();
					int eType = e.getType();
					String aab = e.toString();
					if (eType == EdgeTypes.GOTO_EDGE) {
						BasicBlock target = e.getTarget();
						InstructionHandle targetFirst = getDeepFirstInstruction(cfg, target);
						if (targetFirst != null) {
							int targetPos = targetFirst.getPosition();
							if (targetPos > maxGoto) maxGoto = targetPos;
						}	
					}
				}
			} else if (last!=null && myPos==bb.getFirstInstruction().getPosition()) {
				// note: getFirstInstruction() may return null, but won't if last!=null.
				myBB = bb; // used in case (c) below
			}
		}
		/* ok, there are three cases:
		 * a) maxGoto == myPos: There is no default case within the switch.
		 * b) maxGoto > myPos: maxGoto is the end (exclusive) of the default case
		 * c) maxGoto < myPos: all the cases do something like return or throw, so
		 *                     who knows if there is a default case (and it's length)? */
		if (maxGoto < myPos && myBB != null) {
			/* We're in case (c), so let's guess that the end of the basic block
			 * is the end of the default case (if it exists). This may give false
			 * negatives (if the default case has branches, for example) but
			 * shouldn't give false negatives (because if it matches one of the
			 * cases, then it has also matched that case's return/throw). */
			InstructionHandle last = myBB.getLastInstruction();
			if (last != null) {
				// note: last.getNext() may return null, so do it this way
				return last.getPosition() + last.getInstruction().getLength();
			}
		}
		return maxGoto;
	}

	private byte[] getCodeBytes(Method m, int start, int end) {
		byte[] code = m.getCode().getCode();
		byte[] bytes = new byte[end-start];
		System.arraycopy( code, start, bytes, 0, end - start);

		try {
			ByteSequence sequence = new ByteSequence(code);
			while ((sequence.available() > 0) && (sequence.getIndex() < start)) {
				Instruction.readInstruction(sequence);
			}

			int pos;
			while (sequence.available() > 0 && ((pos = sequence.getIndex()) < end)) {
				Instruction ins = Instruction.readInstruction(sequence);
				if ((ins instanceof BranchInstruction)
				&&  !(ins instanceof TABLESWITCH)
				&&  !(ins instanceof LOOKUPSWITCH)) {
					BranchInstruction bi = (BranchInstruction)ins;
					int offset = bi.getIndex();
					int target = offset + pos;
					if (target >= end) { // or target < start ??
						byte hiByte = (byte)((target >> 8) & 0x000000FF);
						byte loByte = (byte)(target & 0x000000FF);
						bytes[pos+bi.getLength()-2 - start] = hiByte;
						bytes[pos+bi.getLength()-1 - start] = loByte;
					}
				}
			}
		} catch (IOException ioe) {
		}

		return bytes;
	}

	private InstructionHandle findThenFinish(CFG cfg, BasicBlock thenBB, int elsePos) {
		InstructionHandle inst = thenBB.getFirstInstruction();
		while (inst == null) {
			Iterator<Edge> ie = cfg.outgoingEdgeIterator(thenBB);
			while (ie.hasNext()) {
				Edge e = ie.next();
				if (e.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
					thenBB = e.getTarget();
					break;
				}
			}
			inst = thenBB.getFirstInstruction();
		}

		InstructionHandle lastIns = inst;
		while (inst.getPosition() < elsePos) {
			lastIns = inst;
			inst = inst.getNext();
		}

		return lastIns;
	}

	public void report() {
	}
}
