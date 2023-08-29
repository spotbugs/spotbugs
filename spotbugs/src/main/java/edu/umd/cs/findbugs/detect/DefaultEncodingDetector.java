package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.AnnotationEnumeration;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.Values;

/**
 * <p>
 * Finds invocations of JDK methods that rely on the default platform encoding.
 * </p>
 * <p>
 * If a Java application assumes that the default platform encoding is
 * acceptable, the app's behaviour will vary from platform to platform. In
 * particular, conversions between byte[] and java.lang.String (in either
 * direction) may yield inconsistent results. To ensure Java code is portable,
 * the desired encoding should be specified explicitly wherever such a
 * conversion takes place.
 * </p>
 * <p>
 * This FindBugs pattern detects invocations of Java Class Library methods and
 * constructors that are known to use the default platform encoding.
 * </p>
 *
 * @author Robin Fernandes
 */
public class DefaultEncodingDetector extends OpcodeStackDetector {

    private final BugAccumulator bugAccumulator;

    private final DefaultEncodingAnnotationDatabase defaultEncodingAnnotationDatabase;

    /**
     * This annotation is used to denote a method which relies on the default
     * platform encoding.
     */
    static class DefaultEncodingAnnotation extends AnnotationEnumeration<DefaultEncodingAnnotation> {
        public final static DefaultEncodingAnnotation DEFAULT_ENCODING = new DefaultEncodingAnnotation("DefaultEncoding", 1);

        private final static DefaultEncodingAnnotation[] myValues = { DEFAULT_ENCODING };

        public static DefaultEncodingAnnotation[] values() {
            return myValues.clone();
        }

        private DefaultEncodingAnnotation(String s, int i) {
            super(s, i);
        }
    }

    /**
     * Sets up and stores DefaultEncodingAnnotations on JCL methods.
     */
    static class DefaultEncodingAnnotationDatabase extends AnnotationDatabase<DefaultEncodingAnnotation> {

        public DefaultEncodingAnnotationDatabase() {
            this.setAddClassOnly(false);
            this.loadAuxiliaryAnnotations();
        }

        Set<ClassDescriptor> classes = new HashSet<>();

        @Override
        protected void addMethodAnnotation(@DottedClassName String cName, String mName, String mSig, boolean isStatic,
                DefaultEncodingAnnotation annotation) {
            super.addMethodAnnotation(cName, mName, mSig, isStatic, annotation);
            classes.add(DescriptorFactory.createClassDescriptorFromDottedClassName(cName));

        }

        @Override
        public void loadAuxiliaryAnnotations() {
            addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "getBytes", "()[B", false, DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, Const.CONSTRUCTOR_NAME, "([B)V", false, DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, Const.CONSTRUCTOR_NAME, "([BII)V", false, DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.ByteArrayOutputStream", "toString", "()Ljava/lang/String;", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileReader", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileReader", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileReader", Const.CONSTRUCTOR_NAME, "(Ljava/io/FileDescriptor;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileWriter", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileWriter", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;Z)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;Z)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.FileWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/FileDescriptor;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.InputStreamReader", Const.CONSTRUCTOR_NAME, "(Ljava/io/InputStream;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.OutputStreamWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/OutputStream;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintStream", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintStream", Const.CONSTRUCTOR_NAME, "(Ljava/io/OutputStream;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintStream", Const.CONSTRUCTOR_NAME, "(Ljava/io/OutputStream;Z)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintStream", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/OutputStream;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintWriter", Const.CONSTRUCTOR_NAME, "(Ljava/io/OutputStream;Z)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.io.PrintWriter", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.util.Scanner", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.util.Scanner", Const.CONSTRUCTOR_NAME, "(Ljava/io/InputStream;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.util.Scanner", Const.CONSTRUCTOR_NAME, "(Ljava/nio/channels/ReadableByteChannel;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.util.Formatter", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.util.Formatter", Const.CONSTRUCTOR_NAME, "(Ljava/io/File;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.util.Formatter", Const.CONSTRUCTOR_NAME, "(Ljava/io/OutputStream;)V", false,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);

            addMethodAnnotation("java.nio.file.Files", "readString", "(Ljava/nio/file/Path;)Ljava/lang/String;", true,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.nio.file.Files", "readAllLines", "(Ljava/nio/file/Path;)Ljava/util/List;", true,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.nio.file.Files", "writeString",
                    "(Ljava/nio/file/Path;Ljava/lang/CharSequence;[Ljava/nio/file/OpenOption;)Ljava/nio/file/Path;", true,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.nio.file.Files", "newBufferedReader", "(Ljava/nio/file/Path;)Ljava/io/BufferedReader;", true,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
            addMethodAnnotation("java.nio.file.Files", "newBufferedWriter",
                    "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;", true,
                    DefaultEncodingAnnotation.DEFAULT_ENCODING);
        }

    }

    public DefaultEncodingDetector(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.defaultEncodingAnnotationDatabase = new DefaultEncodingAnnotationDatabase();
    }

    @Override
    public boolean shouldVisit(JavaClass obj) {
        Set<ClassDescriptor> called = getXClass().getCalledClassDescriptors();
        for (ClassDescriptor c : defaultEncodingAnnotationDatabase.classes) {
            if (called.contains(c)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void visit(Code code) {
        super.visit(code); // make callbacks to sawOpcode for all opcodes
        bugAccumulator.reportAccumulatedBugs();

    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            XMethod callSeen = XFactory.createXMethod(MethodAnnotation.fromCalledMethod(this));
            DefaultEncodingAnnotation annotation = defaultEncodingAnnotationDatabase.getDirectAnnotation(callSeen);
            if (annotation != null) {
                bugAccumulator.accumulateBug(new BugInstance(this, "DM_DEFAULT_ENCODING", HIGH_PRIORITY).addClassAndMethod(this)
                        .addCalledMethod(this), this);
            }
            break;
        default:
            break;
        }
    }
}
