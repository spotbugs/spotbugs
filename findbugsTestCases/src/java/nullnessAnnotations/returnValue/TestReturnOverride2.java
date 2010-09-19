package nullnessAnnotations.returnValue;

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class TestReturnOverride2 {

    void a(ACSI o) {
        o.get().toString();
    }

    static class A {
        @CheckForNull
        Object a() {
            return null;
        }
    }

    static class B extends A {
        @Override
        String a() {
            return "B";
        }

        @ExpectWarning("NP")
        int b() {
            return a().hashCode();
        }
    }

    static interface I<K, T extends I.N> {
        @CheckForNull
        T get();

        @CheckForNull
        T get(@Nonnull K k);

        interface N {
        }
    }

    static interface SI<K, T> {
        @CheckForNull
        public T get();
    }

    static interface CSI<K, T> extends SI<K, Collection<T>> {
        @Nonnull
        @Override
        public Collection<T> get();
    }

    static class ACSI implements CSI<Object, String> {

        @ExpectWarning("NP")
        @Override
        public Collection<String> get() {
            return null;
        }

    }

    static class AI implements I<String, AI.AN> {
        @Override
        public AN get(String k) {
            return null;
        }

        @Override
        public AN get() {
            return null;
        }

        @ExpectWarning("NP")
        int ai() {
            return get().hashCode();
        }

        @ExpectWarning("NP")
        Object ai2() {
            return get(null);
        }

        static class AN implements I.N {
        }
    }
}
