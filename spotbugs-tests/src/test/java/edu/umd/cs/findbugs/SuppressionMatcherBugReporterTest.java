package edu.umd.cs.findbugs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuppressionMatcherBugReporterTest {

    @Mock
    private SuppressionMatcher matcher;

    @Mock
    private BugReporter upstreamReporter;

    @InjectMocks
    SuppressionMatcherBugReporter reporter;

    @Test
    void shouldNotCallUpstreamReporterIfBugSuppressed() {
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
    void shouldCallUpstreamReporterIfBugNotSuppressed() {
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
    void shouldCallMatcherAndUpstreamReporterOnFinish() {
        // when
        reporter.finish();
        // then
        verify(matcher).validateSuppressionUsage(reporter);
        verify(upstreamReporter).finish();
    }
}
