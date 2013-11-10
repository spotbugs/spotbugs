package sfBugsNew;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1220 {

    @interface GET {
    }

    @interface PUT {
    }

    @interface POST {
    }

    @interface HEAD {
    }

    /** We get a warning here when compiled with javac, as expected.
     * But eclipse optimizes away the useless jump, so we can't detect anything.
     * Would be nice if expectWarning to say something to the effect of "Warning expected
     * if compiled with javac", but I don't know how to detect which tool generated a classfile.
     */
    @DesireWarning("UCF")
    Annotation getEndpoint(Method targetMethod) {
        Annotation endpoint = null;
        if (null != (endpoint = targetMethod.getAnnotation(GET.class))) {
        } else if (null != (endpoint = targetMethod.getAnnotation(PUT.class))) {
        } else if (null != (endpoint = targetMethod.getAnnotation(POST.class))) {
        } else if (null != (endpoint = targetMethod.getAnnotation(HEAD.class))) {
        }
        return endpoint;
    }

    @NoWarning("UCF")
    Annotation getEndpoint2(Method targetMethod) {
        Annotation endpoint = null;
        if (null != (endpoint = targetMethod.getAnnotation(GET.class))) {
        } else if (null != (endpoint = targetMethod.getAnnotation(PUT.class))) {
        } else if (null != (endpoint = targetMethod.getAnnotation(POST.class))) {
        } else {
            endpoint = targetMethod.getAnnotation(HEAD.class);
        }
        return endpoint;
    }
}
