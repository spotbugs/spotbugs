package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class ExceptionCompletableFutureTest extends AbstractIntegrationTest {

    @Test
    public void findSwallowedException() {
        performAnalysis("ExceptionCompletableFuture.class");

        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DE_COMPLETABLE_FUTURE").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("DE_COMPLETABLE_FUTURE")
                .inClass("ExceptionCompletableFuture")
                .atJspLine(37)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
