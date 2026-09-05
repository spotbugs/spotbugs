package ghIssues;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

// reproducer from https://github.com/ben-manes/caffeine/blob/master/caffeine/src/jmh/java/com/github/benmanes/caffeine/FactoryBenchmark.java
public class Issue3320 {
    static final class MethodHandleFactory {

        private final MethodHandle methodHandle;

        MethodHandleFactory() {
            try {
                methodHandle = MethodHandles.lookup().findConstructor(Alpha.class, MethodType.methodType(void.class));
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        Alpha invoke() {
            try {
                return (Alpha) methodHandle.invoke();
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static final class Alpha {
        public Alpha() {}
    }
}
