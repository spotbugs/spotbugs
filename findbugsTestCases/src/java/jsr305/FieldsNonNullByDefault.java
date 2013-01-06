package jsr305;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Untainted;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.NoWarning;

@Nonnull(when = When.ALWAYS)
@TypeQualifierDefault(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldsNonNullByDefault {

}
