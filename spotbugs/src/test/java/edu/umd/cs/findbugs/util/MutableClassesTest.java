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
    Annotated set(int n) {
        return new Annotated(n);
    }
}

public class MutableClassesTest {
    @Test
    public void TestKnownMutable() {
        Assert.assertTrue(MutableClasses.mutableSignature("Ljava/util/Date;"));
    }

    @Test
    public void TestKnownImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/lang/String;"));
    }

    @Test
    public void TestKnownImmutablePackage() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ljava/time/LocalTime;"));
    }

    @Test
    public void TestNamedImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Lcom/google/common/collect/ImmutableMap;"));
    }

    @Test
    public void TestAnnotatedImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/Annotated;"));
    }

    @Test
    public void TestArray() {
        Assert.assertTrue(MutableClasses.mutableSignature("[I"));
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
        private static Immutable immutable;
        private int n;

        public Immutable(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }

        public void setN1(int n) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public Immutable setN2(int n) {
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
}
