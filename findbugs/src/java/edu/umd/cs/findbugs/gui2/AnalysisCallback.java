package edu.umd.cs.findbugs.gui2;

/**
 * We use this to know how the analysis went in AnalyzingDialog so we can determine what to do next 
 */
public interface AnalysisCallback
{
	public void analysisInterrupted();
	public void analysisFinished(BugSet results);
}
