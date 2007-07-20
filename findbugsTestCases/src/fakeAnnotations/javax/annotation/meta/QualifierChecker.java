package javax.annotation.meta;

import java.lang.annotation.Annotation;

public interface QualifierChecker<A extends Annotation> {
    /**
     * Given a type qualifier, check to see if a value is an instance of the
     * set of values denoted by the qualifier
     * @param annotation
     * @param annotationArgument
     * @param value
     * @return
     */
	public When forConstantValue(A qualifierqualifierArgument, Object value);
}
