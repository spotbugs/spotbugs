package edu.umd.cs.findbugs.ba.ca;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ReversePostfixOrder;

public class CallListAnalysis extends AbstractDataflowAnalysis<CallList> {
	private CFG cfg;
	private DepthFirstSearch dfs;
	
	public CallListAnalysis(CFG cfg, DepthFirstSearch dfs) {
		this.cfg = cfg;
		this.dfs = dfs;
	}
	
	public void initEntryFact(CallList fact) {
		fact.setTop();
	}
	
	public void initResultFact(CallList fact) {
		fact.clear();
	}
	
	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostfixOrder(cfg, dfs);
	}
	
	public void makeFactTop(CallList fact) {
		fact.setTop();
	}
	
	public CallList createFact() {
		return new CallList();
	}
	
	public boolean same(CallList a, CallList b) {
		return a.equals(b);
	}
	
	public void meetInto(CallList start, Edge edge, CallList result)
			throws DataflowAnalysisException {
		CallList merge = CallList.merge(start, result);
		result.copyFrom(merge);
	}
	
	public void copy(CallList source, CallList dest) {
		dest.copyFrom(source);
	}

	public void transferInstruction(
			InstructionHandle handle, BasicBlock basicBlock, CallList fact) throws DataflowAnalysisException {
		// TODO: implement
	}
	public boolean isFactValid(CallList fact) {
		return !(fact.isTop() || fact.isBottom());
	}
}
