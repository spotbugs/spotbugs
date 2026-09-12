package nullnessAnnotations.returnValue;

import java.util.Collection;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestReturnOverride2 {

    void a(ACSI o) {
        o.get().toString();
    }

    static class A {
        @Nullable
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
        @Nullable
        T get();

        @Nullable
        T get(@NonNull K k);

        interface N {
        }
    }

    static interface SI<K, T> {
        public @Nullable T get();
    }

    static interface CSI<K, T> extends SI<K, Collection<T>> {
        @Override
        public @NonNull Collection<T> get();
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
