package edu.umd.cs.findbugs.util;

import javax.annotation.concurrent.Immutable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

@Immutable
class Annotated {
    int n;

    Annotated(int n) {
        this.n = n;
    }

    // False setter
    void set(int n) {
        System.out.println("This is not a setter. So we do not set n to " + n + ".");
    }
}

class MutableClassesTest {

    @Test
    void testKnownMutable() {
        Assertions.assertTrue(MutableClasses.mutableSignature("Ljava/util/Date;"));
    }

    @Test
    void testKnownImmutablePackage() {
        Assertions.assertFalse(MutableClasses.mutableSignature("Ljava/time/LocalTime;"));
    }

    @Test
    void testKnownImmutable() {
        Assertions.assertFalse(MutableClasses.mutableSignature("Ljava/lang/String;"));
        Assertions.assertFalse(MutableClasses.mutableSignature("Ljava/util/regex/Pattern;"));
    }

    @Test
    void testLocale() {
        Assertions.assertFalse(MutableClasses.mutableSignature("Ljava/util/Locale;"));
    }

    @Test
    void testArray() {
        Assertions.assertTrue(MutableClasses.mutableSignature("[I"));
    }

    @Test
    void testAnnotatedImmutable() {
        Assertions.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/Annotated;"));
    }

    public static class Mutable {
        private int n;

        public Mutable(int n) {
            this.n = n;
        }

        public void setN(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }
    }

    @Test
    void testMutable() {
        Assertions.assertTrue(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$Mutable;"));
    }

    public static class Immutable {
        private int n;
        private static Immutable immutable;

        public Immutable(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }

        public Immutable setN(int n) {
            return new Immutable(n);
        }

        public static Immutable getImmutable() {
            return immutable;
        }

        public static void setImmutable(Immutable imm) {
            immutable = imm;
        }
    }

    @Test
    void testImmutable() {
        Assertions.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$Immutable;"));
    }

    // This tests fails on java 8/11, so disable it and only run on java 17+ unless its determined how to fix
    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testImmutableValuedBased() {
        // Annotated with @jdk.internal.ValueBased and has "setValue", which should normally trip detection
        System.out.println("starting.....");
        Assertions.assertFalse(MutableClasses.mutableSignature("Ljava/util/KeyValueHolder;"));
    }

    @com.google.errorprone.annotations.Immutable
    public static class ErrorProneImmutable {
        public void write() {
            // Does not matter
        }
    }

    public static class ErrorProneImmutableSubclass extends ErrorProneImmutable {
        public void writeOther() {
            // Does not matter
        }
    }

    @Test
    void testEnumsAreImmutable() {
        Assertions.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$ImmutableTestEnum;"));
    }

    public enum ImmutableTestEnum {
        ONE,
        TWO;

        public void write() {
            // Does not matter
        }
    }

    @Test
    void testErrorProneImmutable() {
        Assertions.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ErrorProneImmutable;"));
        Assertions.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ErrorProneImmutableSubclass;"));
    }
}
