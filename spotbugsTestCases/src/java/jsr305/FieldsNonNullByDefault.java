package jsr305;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.Nonnull;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierDefault;
import com.github.spotbugs.jsr305.annotation.meta.When;

@Nonnull(when = When.ALWAYS)
@TypeQualifierDefault(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldsNonNullByDefault {

}
