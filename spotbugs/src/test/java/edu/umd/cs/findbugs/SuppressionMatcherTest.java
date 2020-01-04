package edu.umd.cs.findbugs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SuppressionMatcherTest {

    private static final String PACKAGE_NAME = "com.example";
    private static final String CLASS_NAME = PACKAGE_NAME + ".Test";
    private static final ClassAnnotation CLASS_ANNOTATION = new ClassAnnotation(CLASS_NAME);

    private SuppressionMatcher matcher;
    private BugReporter bugReporter;
    private ArgumentCaptor<BugInstance> bugCaptor;

    @Before
    public void init() {
        matcher = new SuppressionMatcher();
        bugReporter = mock(BugReporter.class);
        bugCaptor = ArgumentCaptor.forClass(BugInstance.class);
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }

    @Test
    public void shouldMatchClassLevelSuppression() {
        // given
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION));
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME);
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    public void shouldMatchPackageLevelSuppressor() {
        // given
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME);
        matcher.addPackageSuppressor(new PackageWarningSuppressor("UUF_UNUSED_FIELD", PACKAGE_NAME));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    public void shouldMatchMethodLevelSuppressor() {
        // given
        MethodAnnotation method = new MethodAnnotation(CLASS_NAME, "test", "bool test()", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME).addMethod(method);
        matcher.addSuppressor(new MethodWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION, method));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    public void shouldMatchParameterLevelSuppressor() {
        // given
        MethodAnnotation method = new MethodAnnotation(CLASS_NAME, "test", "bool test()", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1)
                .addClass(CLASS_NAME).addMethod(method)
                .addAnnotations(Collections.singletonList(new LocalVariableAnnotation("?", 2, 0, 0)));
        matcher.addSuppressor(new ParameterWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION, method, 2));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    public void shouldMatchFieldLevelSuppressor() {
        // given
        FieldAnnotation field = new FieldAnnotation(CLASS_NAME, "test", "bool test", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME).addField(field);
        matcher.addSuppressor(new FieldWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION, field));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    public void shouldNotMatchBugsWithDifferentType() {
        // given
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION));
        BugInstance bug = new BugInstance("UWF_NULL_FIELD", 1).addClass(CLASS_NAME);
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should not match the bug", matched, is(false));
    }

    @Test
    public void shouldNotBreakOnMissingPrimaryClass() {
        // given
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1);
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should not match the bug", matched, is(false));
    }

    @Test
    public void shouldMatchFieldSuppressorBeforeClass() {
        // given
        FieldAnnotation field = new FieldAnnotation(CLASS_NAME, "test", "bool test", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME).addField(field);
        matcher.addSuppressor(new FieldWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION, field));
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION));
        // when
        matcher.match(bug);
        matcher.validateSuppressionUsage(bugReporter);
        // then
        verify(bugReporter).reportBug(bugCaptor.capture());
        assertThat("Bug type", bugCaptor.getValue().getBugPattern().getType(), startsWith("US_USELESS_SUPPRESSION_ON_CLASS"));
    }

    @Test
    public void shouldReportUselessSuppressor() {
        // given
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME);
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", CLASS_ANNOTATION));
        matcher.addSuppressor(new ClassWarningSuppressor("UWF_NULL_FIELD", CLASS_ANNOTATION));
        // when
        matcher.match(bug);
        matcher.validateSuppressionUsage(bugReporter);
        // then
        verify(bugReporter).reportBug(bugCaptor.capture());
        assertThat("Bug type", bugCaptor.getValue().getBugPattern().getType(), startsWith("US_USELESS_SUPPRESSION"));
    }
}
