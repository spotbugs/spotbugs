package annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.umd.cs.findbugs.Priorities;

/**
 * This annotation documents to which detector(s) annotated test samples are linked.
 * Currently it is used for documentation only.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface DetectorUnderTest {
    Class<? extends Priorities> [] value();
}
