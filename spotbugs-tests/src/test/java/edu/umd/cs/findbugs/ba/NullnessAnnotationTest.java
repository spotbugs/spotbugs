package edu.umd.cs.findbugs.ba;

import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Verifies {@link NullnessAnnotation} parser.
 *
 * @author kzaikin
 */
class NullnessAnnotationTest {

    private static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of("android.support.annotation.NonNull", NullnessAnnotation.NONNULL),
                Arguments.of("android.support.annotation.Nullable", NullnessAnnotation.CHECK_FOR_NULL),

                // there is no such thing as com.google.common.base.NonNull
                Arguments.of("com.google.common.base.Nullable", NullnessAnnotation.CHECK_FOR_NULL),

                Arguments.of("org.eclipse.jdt.annotation.NonNull", NullnessAnnotation.NONNULL),
                Arguments.of("org.eclipse.jdt.annotation.Nullable", NullnessAnnotation.CHECK_FOR_NULL),

                Arguments.of("org.jetbrains.annotations.NotNull", NullnessAnnotation.NONNULL),
                Arguments.of("org.jetbrains.annotations.Nullable", NullnessAnnotation.CHECK_FOR_NULL),

                Arguments.of("org.checkerframework.checker.nullness.qual.Nullable", NullnessAnnotation.CHECK_FOR_NULL),
                Arguments.of("org.checkerframework.checker.nullness.compatqual.NullableDecl", NullnessAnnotation.CHECK_FOR_NULL),

                Arguments.of(edu.umd.cs.findbugs.annotations.CheckForNull.class.getName(), NullnessAnnotation.CHECK_FOR_NULL),
                Arguments.of(edu.umd.cs.findbugs.annotations.PossiblyNull.class.getName(), NullnessAnnotation.CHECK_FOR_NULL),

                Arguments.of(javax.annotation.CheckForNull.class.getName(), NullnessAnnotation.CHECK_FOR_NULL),
                Arguments.of(javax.annotation.Nonnull.class.getName(), NullnessAnnotation.NONNULL),
                Arguments.of(javax.annotation.Nullable.class.getName(), NullnessAnnotation.NULLABLE),

                Arguments.of("something different", null));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void annotationRecognition(String annotation, NullnessAnnotation ofType) {
        assertThat(NullnessAnnotation.Parser.parse(annotation), is(ofType));
    }
}
