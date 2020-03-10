package ghIssues;

/*
    Check with something like this ant task

    <target name="spotbugs-issue-1025" depends="">
        <taskdef
            resource="edu/umd/cs/findbugs/anttask/tasks.properties"
            classpath="${spotbugs.home}/lib/spotbugs-ant.jar" />

        <spotbugs home="${spotbugs.home}"
            reportLevel="low"
            output="html"
            outputFile="spotbugs-issue-1025.html" >
            <sourcePath>
                <fileset dir="${src.dir}">
                    <include name="ghIssues/Issue-1025.java"/>
                </fileset>
            </sourcePath>
            <class location="${classes.dir}/ghIssues"/>
        </spotbugs>
    </target>

spotbugs-issue-1025:
 [spotbugs] Executing SpotBugs FindBugsTask from ant task
 [spotbugs] Running SpotBugs...
 [spotbugs] Type error evaluating ((./Class)/@classname) in xsl:sort/@select on line 285 column 41 of default.xsl:
 [spotbugs]   XTTE1020: A sequence of more than one item is not allowed as the @select attribute of
 [spotbugs]   xsl:sort (@classname="ghIssues.Issue1025$One", @classname="ghIssues.Issue1025$Two")
 [spotbugs] at template generateWarningTable on line 275 of default.xsl:
 [spotbugs]      invoked by xsl:call-template at file:///[....]/building/default.xsl#203
 [spotbugs]      invoked by unknown caller (class net.sf.saxon.expr.instruct.ForEach) at file:///[....]/building/default.xsl#198
 [spotbugs]   In template rule with match="/" on line 69 of default.xsl
 [spotbugs] The following errors occurred during analysis:
 [spotbugs]   Could not generate HTML output
 [spotbugs]     net.sf.saxon.trans.XPathException: A sequence of more than one item is not allowed as the @select attribute of xsl:sort (@classname="ghIssues.Issue1025$One", @classname="ghIssues.Issue1025$Two")
 [spotbugs]       At net.sf.saxon.expr.Expression.typeError(Expression.java:1464)
 [spotbugs]       At net.sf.saxon.expr.SingletonAtomizer.evaluateItem(SingletonAtomizer.java:221)
 [spotbugs]       At net.sf.saxon.expr.SingletonAtomizer.evaluateItem(SingletonAtomizer.java:31)
 [spotbugs]       At net.sf.saxon.expr.sort.SortExpression.evaluateSortKey(SortExpression.java:394)
 [spotbugs]       At net.sf.saxon.expr.sort.SortedIterator.buildArray(SortedIterator.java:215)
 [spotbugs]       At net.sf.saxon.expr.sort.SortedIterator.doSort(SortedIterator.java:231)
 [spotbugs]       At net.sf.saxon.expr.sort.SortedIterator.next(SortedIterator.java:148)
 [spotbugs]       At net.sf.saxon.om.FocusTrackingIterator.next(FocusTrackingIterator.java:73)
 [spotbugs]       At net.sf.saxon.trans.Mode.applyTemplates(Mode.java:455)
 [spotbugs]       At net.sf.saxon.expr.instruct.ApplyTemplates.apply(ApplyTemplates.java:300)
 [spotbugs]       At net.sf.saxon.expr.instruct.ApplyTemplates.processLeavingTail(ApplyTemplates.java:255)
 [spotbugs]       At net.sf.saxon.expr.instruct.Block.processLeavingTail(Block.java:735)
 [spotbugs]       At net.sf.saxon.expr.instruct.Instruction.process(Instruction.java:132)
 [spotbugs]       At net.sf.saxon.expr.instruct.ElementCreator.processLeavingTail(ElementCreator.java:352)
 [spotbugs]       At net.sf.saxon.expr.instruct.ElementCreator.processLeavingTail(ElementCreator.java:299)
 [spotbugs]       At net.sf.saxon.expr.instruct.Block.processLeavingTail(Block.java:735)
 [spotbugs]       At net.sf.saxon.expr.instruct.NamedTemplate.expand(NamedTemplate.java:243)
 [spotbugs]       At net.sf.saxon.expr.instruct.CallTemplate.process(CallTemplate.java:353)
 [spotbugs]       At net.sf.saxon.expr.LetExpression.process(LetExpression.java:608)
 [spotbugs]       At net.sf.saxon.expr.instruct.ForEach.lambda$processLeavingTail$0(ForEach.java:484)
 [spotbugs]       At net.sf.saxon.om.SequenceIterator.forEachOrFail(SequenceIterator.java:128)
 [spotbugs]       At net.sf.saxon.expr.instruct.ForEach.processLeavingTail(ForEach.java:484)
 [spotbugs]       At net.sf.saxon.expr.instruct.Block.processLeavingTail(Block.java:735)
 [spotbugs]       At net.sf.saxon.expr.instruct.Instruction.process(Instruction.java:132)
 [spotbugs]       At net.sf.saxon.expr.instruct.ElementCreator.processLeavingTail(ElementCreator.java:352)
 [spotbugs]       At net.sf.saxon.expr.instruct.ElementCreator.processLeavingTail(ElementCreator.java:299)
 [spotbugs]       At net.sf.saxon.expr.LetExpression.processLeavingTail(LetExpression.java:721)
 [spotbugs]       At net.sf.saxon.expr.instruct.Block.processLeavingTail(Block.java:735)
 [spotbugs]       At net.sf.saxon.expr.instruct.Instruction.process(Instruction.java:132)
 [spotbugs]       At net.sf.saxon.expr.instruct.ElementCreator.processLeavingTail(ElementCreator.java:352)
 [spotbugs]       At net.sf.saxon.expr.instruct.ElementCreator.processLeavingTail(ElementCreator.java:299)
 [spotbugs]       At net.sf.saxon.expr.instruct.TemplateRule.applyLeavingTail(TemplateRule.java:352)
 [spotbugs]       At net.sf.saxon.trans.Mode.applyTemplates(Mode.java:532)
 [spotbugs]       At net.sf.saxon.trans.XsltController.applyTemplates(XsltController.java:747)
 [spotbugs]       At net.sf.saxon.s9api.AbstractXsltTransformer.applyTemplatesToSource(AbstractXsltTransformer.java:347)
 [spotbugs]       At net.sf.saxon.s9api.XsltTransformer.transform(XsltTransformer.java:349)
 [spotbugs]       At net.sf.saxon.jaxp.TransformerImpl.transform(TransformerImpl.java:71)
 [spotbugs]       At edu.umd.cs.findbugs.HTMLBugReporter.finish(HTMLBugReporter.java:73)
 [spotbugs]       At edu.umd.cs.findbugs.DelegatingBugReporter.finish(DelegatingBugReporter.java:89)
 [spotbugs]       At edu.umd.cs.findbugs.DelegatingBugReporter.finish(DelegatingBugReporter.java:89)
 [spotbugs]       At edu.umd.cs.findbugs.FindBugs2.analyzeApplication(FindBugs2.java:1165)
 [spotbugs]       At edu.umd.cs.findbugs.FindBugs2.execute(FindBugs2.java:309)
 [spotbugs]       At edu.umd.cs.findbugs.FindBugs.runMain(FindBugs.java:395)
 [spotbugs]       At edu.umd.cs.findbugs.FindBugs2.main(FindBugs2.java:1231)
 [spotbugs]
 [spotbugs] Java Result: 1
 [spotbugs] Output saved to spotbugs-issue-1025.html

  Running the xml report reveals

    <BugInstance type="NM_CONFUSING" priority="3" rank="19" abbrev="Nm" category="BAD_PRACTICE">
    <Class classname="ghIssues.Issue1025$One">
      <SourceLine classname="ghIssues.Issue1025$One" start="26" end="27" sourcefile="Issue1025.java" sourcepath="ghIssues/Issue1025.java"/>
    </Class>
    <Method classname="ghIssues.Issue1025$One" name="methodCapitizationCheck" signature="()V" isStatic="false">
      <SourceLine classname="ghIssues.Issue1025$One" start="27" end="27" startBytecode="0" endBytecode="42" sourcefile="Issue1025.java" sourcepath="ghIs
sues/Issue1025.java"/>
    </Method>
    <Class classname="ghIssues.Issue1025$Two">
      <SourceLine classname="ghIssues.Issue1025$Two" start="30" end="31" sourcefile="Issue1025.java" sourcepath="ghIssues/Issue1025.java"/>
    </Class>
    <Method classname="ghIssues.Issue1025$Two" name="methodCapitizationCHECK" signature="()V" isStatic="false">
      <SourceLine classname="ghIssues.Issue1025$Two" start="31" end="31" startBytecode="0" endBytecode="42" sourcefile="Issue1025.java" sourcepath="ghIs
sues/Issue1025.java"/>
    </Method>
  </BugInstance>

 */
public class Issue1025 {

    static class One {
        public void methodCapitizationCheck() {}
    }

    static class Two {
        public void methodCapitizationCHECK() {}
    }
}
