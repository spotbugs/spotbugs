package edu.umd.cs.findbugs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SuppressionMatcherBugReporterTest {

    @Mock
    private SuppressionMatcher matcher;

    @Mock
    private BugReporter upstreamReporter;

    @InjectMocks
    SuppressionMatcherBugReporter reporter;

    @Test
    public void shouldNotCallUpstreamReporterIfBugSuppressed() {
        // given
        BugInstance suppressed = mock(BugInstance.class);
        when(matcher.match(suppressed)).thenReturn(true);
        // when
        reporter.reportBug(suppressed);
        // then
        verify(matcher).match(suppressed);
        verifyNoInteractions(upstreamReporter);
    }

    @Test
    public void shouldCallUpstreamReporterIfBugNotSuppressed() {
        // given
        BugInstance reported = mock(BugInstance.class);
        when(matcher.match(reported)).thenReturn(false);
        // when
        reporter.reportBug(reported);
        // then
        verify(matcher).match(reported);
        verify(upstreamReporter).reportBug(reported);
    }

    @Test
    public void shouldCallMatcherAndUpstreamReporterOnFinish() {
        // when
        reporter.finish();
        // then
        verify(matcher).validateSuppressionUsage(reporter);
        verify(upstreamReporter).finish();
    }
}
