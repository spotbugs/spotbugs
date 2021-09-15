package edu.umd.cs.findbugs.util;

import javax.annotation.concurrent.Immutable;

import org.junit.Assert;
import org.junit.Test;

class MutableClass {
    private int n;

    public MutableClass(int n) {
        this.n = n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getN() {
        return n;
    }
}

class ImmutableClass {
    private int n;
    private static ImmutableClass immutable;

    public ImmutableClass(int n) {
        setNPrivate(n);
    }

    public int getN() {
        return n;
    }

    public ImmutableClass setN(int n) {
        return new ImmutableClass(n);
    }

    private void setNPrivate(int n) {
        this.n = n;
    }

    public static ImmutableClass getImmutable() {
        return immutable;
    }

    public static void setImmutable(ImmutableClass imm) {
        immutable = imm;
    }
}

@Immutable
class AnnotatedClass {
    int n;

    AnnotatedClass(int n) {
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
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/AnnotatedClass;"));
    }

    @Test
    public void TestMutable() {
        Assert.assertTrue(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/MutableClass;"));
    }

    @Test
    public void TestImmutable() {
        Assert.assertFalse(MutableClasses.mutableSignature("Ledu/umd/cs/findbugs/util/ImmutableClass;"));
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
