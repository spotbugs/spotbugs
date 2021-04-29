package edu.umd.cs.findbugs.util;

import org.junit.Assert;
import org.junit.Test;

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
        private int n;

        public Immutable(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }

        public Immutable setN(int n) {
            return new Immutable(n);
        }
    }

    @Test
    public void TestImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClassesTest$Immutable;"));
    }
}
