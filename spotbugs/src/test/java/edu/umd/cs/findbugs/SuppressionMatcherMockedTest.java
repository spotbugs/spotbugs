package edu.umd.cs.findbugs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SuppressionMatcherMockedTest {

    @Mock
    private ClassAnnotation classAnnotation;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ClassWarningSuppressor suppressor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BugInstance bugInstance;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BugInstance uselessSuppressionBugInstance;

    @Mock
    private BugReporter bugReporter;

    @Before
    public void init() {
        when(suppressor.getClassAnnotation().getTopLevelClass()).thenReturn(classAnnotation);
        when(bugInstance.getPrimaryClass().getTopLevelClass()).thenReturn(classAnnotation);
    }

    @Test
    public void shouldMatchBugInstance() {
        // given
        when(suppressor.match(bugInstance)).thenReturn(true);
        SuppressionMatcher matcher = new SuppressionMatcher();
        // when
        matcher.addSuppressor(suppressor);
        // then
        assertTrue("Should match BugInstance", matcher.match(bugInstance));
        // and when
        matcher.validateSuppressionUsage(bugReporter);
        // then
        verifyNoInteractions(bugReporter);
    }

    @Test
    public void shouldNotMatchBugInstance() {
        // given
        when(suppressor.match(bugInstance)).thenReturn(false);
        when(suppressor.buildUselessSuppressionBugInstance()).thenReturn(uselessSuppressionBugInstance);
        SuppressionMatcher matcher = new SuppressionMatcher();
        // when
        matcher.addSuppressor(suppressor);
        // then
        assertFalse("Should match BugInstance", matcher.match(bugInstance));
        // and when
        matcher.validateSuppressionUsage(bugReporter);
        // then
        verify(suppressor).buildUselessSuppressionBugInstance();
        verify(bugReporter).reportBug(uselessSuppressionBugInstance);
    }
}
