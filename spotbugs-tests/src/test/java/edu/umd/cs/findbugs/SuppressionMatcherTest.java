package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SuppressionMatcherTest {

    private static final String PACKAGE_NAME = "com.example";
    private static final String CLASS_NAME = PACKAGE_NAME + ".Test";
    private static final ClassAnnotation CLASS_ANNOTATION = new ClassAnnotation(CLASS_NAME);

    private SuppressionMatcher matcher;
    private BugReporter bugReporter;
    private ArgumentCaptor<BugInstance> bugCaptor;

    @BeforeEach
    void init() {
        matcher = new SuppressionMatcher();
        bugReporter = mock(BugReporter.class);
        bugCaptor = ArgumentCaptor.forClass(BugInstance.class);
    }

    @AfterEach
    void validate() {
        Mockito.validateMockitoUsage();
    }

    @Test
    void shouldMatchClassLevelSuppression() {
        // given
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, true));
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME);
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    void shouldMatchPackageLevelSuppressor() {
        // given
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME);
        matcher.addPackageSuppressor(new PackageWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, PACKAGE_NAME, true));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    void shouldMatchMethodLevelSuppressor() {
        // given
        MethodAnnotation method = new MethodAnnotation(CLASS_NAME, "test", "bool test()", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME).addMethod(method);
        matcher.addSuppressor(new MethodWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, method, true, true));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    void shouldMatchParameterLevelSuppressor() {
        // given
        MethodAnnotation method = new MethodAnnotation(CLASS_NAME, "test", "bool test()", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1)
                .addClass(CLASS_NAME).addMethod(method)
                .addAnnotations(Collections.singletonList(new LocalVariableAnnotation("?", 2, 0, 0)));
        matcher.addSuppressor(new ParameterWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, method, 2, true));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    void shouldMatchFieldLevelSuppressor() {
        // given
        FieldAnnotation field = new FieldAnnotation(CLASS_NAME, "test", "bool test", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME).addField(field);
        matcher.addSuppressor(new FieldWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, field, true, true));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should match the bug", matched, is(true));
    }

    @Test
    void shouldNotMatchBugsWithDifferentType() {
        // given
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, true));
        BugInstance bug = new BugInstance("UWF_NULL_FIELD", 1).addClass(CLASS_NAME);
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should not match the bug", matched, is(false));
    }

    @Test
    void shouldNotBreakOnMissingPrimaryClass() {
        // given
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1);
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, true));
        // when
        boolean matched = matcher.match(bug);
        // then
        assertThat("Should not match the bug", matched, is(false));
    }

    @Test
    void shouldMatchFieldSuppressorBeforeClass() {
        // given
        FieldAnnotation field = new FieldAnnotation(CLASS_NAME, "test", "bool test", false);
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME).addField(field);
        matcher.addSuppressor(new FieldWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, field, true, true));
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, true));
        // when
        matcher.match(bug);
        matcher.validateSuppressionUsage(bugReporter, new UselessSuppressionDetector());
        // then
        verify(bugReporter).reportBug(bugCaptor.capture());
        assertThat("Bug type", bugCaptor.getValue().getBugPattern().getType(), startsWith("US_USELESS_SUPPRESSION_ON_CLASS"));
    }

    @Test
    void shouldReportUselessSuppressor() {
        // given
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 1).addClass(CLASS_NAME);
        matcher.addSuppressor(new ClassWarningSuppressor("UUF_UNUSED_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, true));
        matcher.addSuppressor(new ClassWarningSuppressor("UWF_NULL_FIELD", SuppressMatchType.DEFAULT, CLASS_ANNOTATION, true));
        // when
        matcher.match(bug);
        matcher.validateSuppressionUsage(bugReporter, new UselessSuppressionDetector());
        // then
        verify(bugReporter).reportBug(bugCaptor.capture());
        assertThat("Bug type", bugCaptor.getValue().getBugPattern().getType(), startsWith("US_USELESS_SUPPRESSION"));
    }

    @Test
    void uselessSuppressionOnClassMessage() {
        ClassWarningSuppressor suppressor = new ClassWarningSuppressor("XYZ", SuppressMatchType.DEFAULT, new ClassAnnotation("java.lang.String"),
                true);
        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is("US: Suppressing annotation XYZ on the class java.lang.String is unnecessary"));
    }

    @Test
    void uselessSuppressionOnFieldMessage() {
        FieldWarningSuppressor suppressor = new FieldWarningSuppressor("XYZ",
                SuppressMatchType.DEFAULT, new ClassAnnotation("java.lang.String"),
                new FieldAnnotation("java.lang.String", "test", "test", false),
                true,
                true);

        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is("US: Suppressing annotation XYZ on the field java.lang.String.test is unnecessary"));
    }

    @Test
    void uselessSuppressionOnMethodMessage() {
        MethodWarningSuppressor suppressor = new MethodWarningSuppressor("XYZ",
                SuppressMatchType.DEFAULT, new ClassAnnotation("java.lang.String"),
                new MethodAnnotation("java.lang.String", "test", "()Ljava/lang/Object;)", false),
                true,
                true);

        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is("US: Suppressing annotation XYZ on the method String.test() is unnecessary"));
    }

    @Test
    void uselessSuppressionOnMethodParameterMessage() {
        ParameterWarningSuppressor suppressor = new ParameterWarningSuppressor("XYZ",
                SuppressMatchType.DEFAULT, new ClassAnnotation("java.lang.String"),
                new MethodAnnotation("java.lang.String", "bar", "()Ljava/lang/Object;)", false),
                0,
                true);

        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is(
                "US: Suppressing annotation XYZ on the parameter 1 of the method String.bar() is unnecessary"));
    }

    @Test
    void uselessSuppressionOnPackageMessage() {
        PackageWarningSuppressor suppressor = new PackageWarningSuppressor("XYZ", SuppressMatchType.DEFAULT, "java.lang", true);
        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is("US: Suppressing annotation XYZ on the package java.lang is unnecessary"));
    }

    @Test
    void nullBugPatternClassWarningSuppressor() {
        ClassWarningSuppressor suppressor = new ClassWarningSuppressor(null, SuppressMatchType.DEFAULT, new ClassAnnotation("java.lang.String"),
                true);
        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is("US: Suppressing annotation on the class java.lang.String is unnecessary"));
    }

    @Test
    void nullBugPatternPackageWarningSuppressor() {
        PackageWarningSuppressor suppressor = new PackageWarningSuppressor("null", SuppressMatchType.DEFAULT, "java.lang", true);
        BugInstance bugInstance = suppressor.buildUselessSuppressionBugInstance(new UselessSuppressionDetector());
        assertThat("Message", bugInstance.getMessage(), is("US: Suppressing annotation on the package java.lang is unnecessary"));
    }
}
