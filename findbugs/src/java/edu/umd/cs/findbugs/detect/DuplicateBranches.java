package edu.umd.cs.findbugs.detect;

import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.BasicBlock.InstructionIterator;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class DuplicateBranches extends PreorderVisitor implements Detector, Constants2
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

	public void visitMethod(Method method) {
		try {
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
				
				if ((thenBB == null) || (elseBB == null))
					continue;
				
				//if (!method.getName().equals("notReportedin086"))
				//	continue;
												
				InstructionIterator thenII = thenBB.instructionIterator();
				InstructionIterator elseII = elseBB.instructionIterator();
				boolean matches = true;
				int codeSize = 0;
				
				while (matches && thenII.hasNext() && elseII.hasNext())
				{
					InstructionHandle ih1 = thenII.next();
					InstructionHandle ih2 = elseII.next();
					if (!ih1.getInstruction().equals(ih2.getInstruction()))
						matches = false;
					codeSize = ih1.getInstruction().getLength();
				}
				
				if (!matches || (codeSize < 2) || thenII.hasNext() || elseII.hasNext())
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
	
	public void report() {
	}
}
