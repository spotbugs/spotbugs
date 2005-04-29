package edu.umd.cs.findbugs.detect;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class DuplicateBranches extends PreorderVisitor implements Detector, StatelessDetector, Constants2
{
	private ClassContext classContext;
	private BugReporter bugReporter;
	
	public DuplicateBranches(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		classContext.getJavaClass().accept(this);
	}

	public void visitMethod(Method method) {
		try {
			if (method.getCode() == null)
				return;
			
			CFG cfg = classContext.getCFG(method);
	
			Iterator<BasicBlock> bbi = cfg.blockIterator();
			while (bbi.hasNext()) {
				BasicBlock bb = bbi.next();
				BasicBlock thenBB = null, elseBB = null;
				
				if (cfg.getNumOutgoingEdges(bb) == 2) {
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
				}
				
				if ((thenBB == null) || (elseBB == null) 
				||  (thenBB.getFirstInstruction() == null) || (elseBB.getFirstInstruction() == null))
					continue;
				
				int thenStartPos = thenBB.getFirstInstruction().getPosition();
				int elseStartPos = elseBB.getFirstInstruction().getPosition();
				
				BasicBlock thenFinishBlock = findThenFinish(cfg, thenBB, elseStartPos);
				
				if (thenFinishBlock == null)
					continue;
				
				Instruction lastFinishIns = thenFinishBlock.getLastInstruction().getInstruction();
				if (!(lastFinishIns instanceof GotoInstruction))
					continue;
				
				int thenFinishPos = thenFinishBlock.getLastInstruction().getPosition();
				int elseFinishPos = ((GotoInstruction) lastFinishIns).getTarget().getPosition();
				
				if (thenFinishPos >= elseStartPos)
					continue;
				
				if ((thenFinishPos - thenStartPos) != (elseFinishPos - elseStartPos))
					continue;
				
				byte[] thenBytes = getCodeBytes(method, thenStartPos, thenFinishPos);
				byte[] elseBytes = getCodeBytes(method, elseStartPos, elseFinishPos);
				
				if (!Arrays.equals(thenBytes, elseBytes))
					continue;
				
				bugReporter.reportBug(new BugInstance(this, "DB_DUPLICATE_BRANCHES", LOW_PRIORITY)
						.addClass(classContext.getJavaClass())
						.addMethod(classContext.getJavaClass().getClassName(), method.getName(), method.getSignature())
						.addSourceLineRange(this, 
								thenBB.getFirstInstruction().getPosition(),
								thenBB.getLastInstruction().getPosition())
						.addSourceLineRange(this, 
								elseBB.getFirstInstruction().getPosition(),
								elseBB.getLastInstruction().getPosition()));
			}
		} catch (Exception e) {
			bugReporter.logError("Failure examining basic blocks in Duplicate Branches detector", e);
		}
	}
	
	private byte[] getCodeBytes(Method m, int start, int end) {
		byte[] code = m.getCode().getCode();
		byte[] bytes = new byte[end-start];
		System.arraycopy( code, start, bytes, 0, end - start);
		return bytes;
	}
	private BasicBlock findThenFinish(CFG cfg, BasicBlock thenBB, int elsePos) {
		//Follow fall thru links until we find a goto link past the else
		
		Iterator<Edge> ie = cfg.outgoingEdgeIterator(thenBB);
		while (ie.hasNext()) {
			Edge e = ie.next();
			if (e.getType() == EdgeTypes.GOTO_EDGE) {
				InstructionHandle firstInsH = e.getTarget().getFirstInstruction();
				if (firstInsH != null) {
					int targetPos = firstInsH.getPosition();
					if (targetPos > elsePos)
						return e.getSource();
				}
			}
		}
		
		ie = cfg.outgoingEdgeIterator(thenBB);
		while (ie.hasNext()) {
			Edge e = ie.next();
			if (e.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
				BasicBlock target = e.getTarget();
				if (target.getFirstInstruction() == null)
					return findThenFinish(cfg, target, elsePos);
				int targetPos = target.getFirstInstruction().getPosition();
				if (targetPos < elsePos)
					return findThenFinish(cfg, target, elsePos);
			}
		}
		return null;
	}
	
	public void report() {
	}
}
