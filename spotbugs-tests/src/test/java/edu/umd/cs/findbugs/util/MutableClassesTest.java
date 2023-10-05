package edu.umd.cs.findbugs.util;

import javax.annotation.concurrent.Immutable;

import org.junit.Assert;
import org.junit.Test;

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

public class MutableClassesTest {
    @Test
    public void TestKnownMutable() {
        Assert.assertTrue(MutableClasses.mutableSignature("Ljava/util/Date;"));
    }

    @Test
    public void TestKnownImmutablePackage() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/time/LocalTime;"));
    }

    @Test
    public void TestKnownImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/lang/String;"));
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/util/regex/Pattern;"));
    }

    @Test
    public void TestLocale() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/util/Locale;"));
    }

    @Test
    public void TestArray() {
        Assert.assertTrue(MutableClasses.mutableSignature("[I"));
    }

    @Test
    public void TestAnnotatedImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/Annotated;"));
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
    public void TestMutable() {
        Assert.assertTrue(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$Mutable;"));
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
    public void TestImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$Immutable;"));
    }

    @Test
    public void TestImmutableValuedBased() {
        // Annotated with @jdk.internal.ValueBased and has "setValue", which should normally trip detection
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/util/KeyValueHolder;"));
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
    public void TestEnumsAreImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$ImmutableTestEnum;"));
    }

    public enum ImmutableTestEnum {
        ONE,
        TWO;

        public void write() {
            // Does not matter
        }
    }

    @Test
    public void TestErrorProneImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ErrorProneImmutable;"));
        Assert.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ErrorProneImmutableSubclass;"));
    }
}
