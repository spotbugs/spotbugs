package jsr305;

import com.github.spotbugs.jsr305.annotation.OverridingMethodsMustInvokeSuper;

public class TestOverridesSuper {

    @OverridingMethodsMustInvokeSuper
    void f() {
    }

    @OverridingMethodsMustInvokeSuper
    void g() {
    }

    static class SubClass extends TestOverridesSuper {
        @Override
        void f() {
            super.f();
        }

        @Override
        void g() {

        }
    }

}
