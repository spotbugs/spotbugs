package edu.umd.cs.findbugs.util;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;

import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.PrintingBugReporter;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.ClassPathImpl;

import net.jcip.annotations.Immutable;

import org.apache.bcel.Repository;
import org.apache.bcel.util.SyntheticRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @BeforeAll
    static void setUp() {
        // When running inside the build other tests might set the spotbugs repository
        Repository.setRepository(SyntheticRepository.getInstance());
    }

    @BeforeEach
    void setUpEach() {
        IAnalysisCache analysisCache = ClassFactory.instance().createAnalysisCache(new ClassPathImpl(), new PrintingBugReporter());
        Global.setAnalysisCacheForCurrentThread(analysisCache);
        FindBugs2.registerBuiltInAnalysisEngines(analysisCache);

        Project project = new Project();
        AnalysisContext analysisContext = new AnalysisContext(project);
        AnalysisContext.setCurrentAnalysisContext(analysisContext);
    }

    @AfterEach
    void teardown() {
        Global.removeAnalysisCacheForCurrentThread();
        AnalysisContext.removeCurrentAnalysisContext();
    }

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
        private final int n;
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

        public void setNUnsupported(int n) {
            throw new UnsupportedOperationException();
        }

        public void setNUnsupported2(int n) {
            throw new UnsupportedOperationException("This class is immutable, setters are unsupported.");
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

    @Test
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

    public static final class MutableWriteReplace {
        Object writeReplace() throws ObjectStreamException {
            return null;
        }
    }

    @Test
    void testMutableWriteReplace() {
        Assertions.assertTrue(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$MutableWriteReplace;"));
    }

    public static final class ImmutableWriteReplace implements Serializable {
        Object writeReplace() throws ObjectStreamException {
            return null;
        }
    }

    @Test
    void testImmutableWriteReplace() {
        Assertions.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ImmutableWriteReplace;"));
    }

    public static final class MutableWriteExternal {
        void writeExternal(ObjectOutput out) {
            // Does not matter
        }
    }

    public static class MutableWriteExternalSig implements Externalizable {
        @Override
        public void writeExternal(ObjectOutput out) {
            // Does not matter
        }

        @Override
        public void readExternal(ObjectInput in) {
            // Does not matter
        }

        void writeExternal() {
            // Does not match signature
        }
    }

    @Test
    void testMutableWriteExternal() {
        Assertions.assertTrue(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$MutableWriteExternal;"));
        Assertions.assertTrue(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$MutableWriteExternalSig;"));
    }

    public static final class ImmutableWriteExternal implements Externalizable {
        @Override
        public void writeExternal(ObjectOutput out) {
            // Does not matter
        }

        @Override
        public void readExternal(ObjectInput in) {
            // Does not matter
        }
    }

    @Test
    void testImmutableWriteExternal() {
        Assertions.assertFalse(MutableClasses.mutableSignature(
                "Ledu/umd/cs/findbugs/util/MutableClassesTest$ImmutableWriteExternal;"));
    }
}
