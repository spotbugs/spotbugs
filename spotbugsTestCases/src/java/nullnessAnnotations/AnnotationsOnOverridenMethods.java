package nullnessAnnotations;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;


public class AnnotationsOnOverridenMethods {

	
	@Nonnull Object foo(@CheckForNull Object o) {
		return o;
	}
	
	Object bar(Object o) {
        return o;
    }
	
	static class Child extends AnnotationsOnOverridenMethods {
		
		
		@Override
		@ExpectWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION,NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
		@CheckForNull Object foo(@Nonnull Object o) {
			return o;
		}
		
		@Override
        @CheckForNull Object bar(@Nonnull Object o) {
            return o;
        }
	}
}
