package edu.umd.cs.findbugs.ba;

import static org.hamcrest.core.Is.is;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Verifies {@link NullnessAnnotation} parser.
 *
 * @author kzaikin
 */
@RunWith(Parameterized.class)
public class NullnessAnnotationTest {

    @Parameterized.Parameters(name = "{0} is {1}")
    public static Object[][] parameters() {
        return new Object[][] {
            { "android.support.annotation.NonNull", NullnessAnnotation.NONNULL },
            { "android.support.annotation.Nullable", NullnessAnnotation.CHECK_FOR_NULL },

            // there is no such thing as com.google.common.base.NonNull
            { "com.google.common.base.Nullable", NullnessAnnotation.CHECK_FOR_NULL },

            { "org.eclipse.jdt.annotation.NonNull", NullnessAnnotation.NONNULL },
            { "org.eclipse.jdt.annotation.Nullable", NullnessAnnotation.CHECK_FOR_NULL },

            { "org.jetbrains.annotations.NotNull", NullnessAnnotation.NONNULL },
            { "org.jetbrains.annotations.Nullable", NullnessAnnotation.CHECK_FOR_NULL },

            { "org.checkerframework.checker.nullness.qual.Nullable", NullnessAnnotation.CHECK_FOR_NULL },
            { "org.checkerframework.checker.nullness.compatqual.NullableDecl", NullnessAnnotation.CHECK_FOR_NULL },

            { edu.umd.cs.findbugs.annotations.CheckForNull.class.getName(), NullnessAnnotation.CHECK_FOR_NULL },
            { edu.umd.cs.findbugs.annotations.PossiblyNull.class.getName(), NullnessAnnotation.CHECK_FOR_NULL },

            { javax.annotation.CheckForNull.class.getName(), NullnessAnnotation.CHECK_FOR_NULL },
            { javax.annotation.Nonnull.class.getName(), NullnessAnnotation.NONNULL },
            { javax.annotation.Nullable.class.getName(), NullnessAnnotation.NULLABLE },

            { "something different", null },
        };
    }

    @Parameterized.Parameter(value = 0)
    public String annotation;

    @Parameterized.Parameter(value = 1)
    public NullnessAnnotation ofType;

    @Test
    public void annotationRecognition() {
        Assert.assertThat(NullnessAnnotation.Parser.parse(annotation), is(ofType));
    }
}
