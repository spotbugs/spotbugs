package edu.umd.cs.findbugs.util;

import javax.annotation.concurrent.Immutable;

import org.junit.Assert;
import org.junit.Test;

class Mutable {
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

class Immutable {
    private int n;

    public Immutable(int n) {
        setNPrivate(n);
    }

    public int getN() {
        return n;
    }

    public Immutable setN(int n) {
        return new Immutable(n);
    }

    private void setNPrivate(int n) {
        this.n = n;
    }

    public static Immutable getImmutable() {
        return immutable;
    }

    public static void setImmutable(Immutable imm) {
        immutable = imm;
    }
}

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
    }

    @Test
    public void TestArray() {
        Assert.assertTrue(MutableClasses.mutableSignature("[I"));
    }

    @Test
    public void TestAnnotatedImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/Annotated;"));
    }

    @Test
    public void TestMutable() {
        Assert.assertTrue(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/Mutable;"));
    }

    @Test
    public void TestImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/Immutable;"));
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
    public void TestErrorProneImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ErrorProneImmutable;"));
        Assert.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ErrorProneImmutableSubclass;"));
    }
}
