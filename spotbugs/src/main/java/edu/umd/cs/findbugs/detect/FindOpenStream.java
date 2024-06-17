/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ResourceCollection;
import edu.umd.cs.findbugs.ResourceTrackingDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.ResourceValueAnalysis;
import edu.umd.cs.findbugs.ba.ResourceValueFrame;

/**
 * A Detector to look for streams that are opened in a method, do not escape the
 * method, and are not closed on all paths out of the method. Note that "stream"
 * is a bit misleading, since we also use the detector to look for database
 * resources that aren't closed.
 *
 * @author David Hovemeyer
 * @author Agustin Toribio atomo@arrakis.es
 */
public final class FindOpenStream extends ResourceTrackingDetector<Stream, StreamResourceTracker> implements StatelessDetector {
    static final boolean DEBUG = SystemProperties.getBoolean("fos.debug");

    static final boolean IGNORE_WRAPPED_UNINTERESTING_STREAMS = !SystemProperties.getBoolean("fos.allowWUS");

    /*
     * ----------------------------------------------------------------------
     * Tracked resource types
     * ----------------------------------------------------------------------
     */

    /**
     * List of base classes of tracked resources.
     */
    static final ObjectType[] streamBaseList = { ObjectTypeFactory.getInstance("java.io.InputStream"),
        ObjectTypeFactory.getInstance("java.io.OutputStream"), ObjectTypeFactory.getInstance("java.util.zip.ZipFile"),
        ObjectTypeFactory.getInstance("java.io.Reader"), ObjectTypeFactory.getInstance("java.io.Writer"),
        ObjectTypeFactory.getInstance("java.sql.Connection"),
        ObjectTypeFactory.getInstance("java.sql.Statement"), ObjectTypeFactory.getInstance("java.sql.ResultSet") };

    /**
     * StreamFactory objects used to detect resources created within analyzed
     * methods.
     */
    static final StreamFactory[] streamFactoryList;

    static {
        ArrayList<StreamFactory> streamFactoryCollection = new ArrayList<>();

        // Examine InputStreams, OutputStreams, Readers, and Writers,
        // ignoring byte array, char array, and String variants.
        streamFactoryCollection.add(new IOStreamFactory("java.io.InputStream", new String[] { "java.io.ByteArrayInputStream",
            "java.io.StringBufferInputStream", "java.io.PipedInputStream" }, "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new IOStreamFactory("java.io.OutputStream", new String[] { "java.io.ByteArrayOutputStream",
            "java.io.PipedOutputStream" }, "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new IOStreamFactory("java.io.Reader", new String[] { "java.io.StringReader",
            "java.io.CharArrayReader", "java.io.PipedReader" }, "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new IOStreamFactory("java.io.Writer", new String[] { "java.io.StringWriter",
            "java.io.CharArrayWriter", "java.io.PipedWriter" }, "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new IOStreamFactory("java.util.zip.ZipFile", new String[0], "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.lang.Class", "getResourceAsStream",
                "(Ljava/lang/String;)Ljava/io/InputStream;", "OS_OPEN_STREAM"));

        // Added support for java.nio.file.Files (since 1.7)
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newInputStream",
                "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/InputStream;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newOutputStream",
                "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/OutputStream;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newByteChannel",
                "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/nio/channels/SeekableByteChannel;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newByteChannel",
                "(Ljava/nio/file/Path;Ljava/util/Set;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/channels/SeekableByteChannel;",
                "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newDirectoryStream",
                "(Ljava/nio/file/Path;)Ljava/nio/file/DirectoryStream;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newDirectoryStream",
                "(Ljava/nio/file/Path;Ljava/nio/file/DirectoryStream$Filter;)Ljava/nio/file/DirectoryStream;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newDirectoryStream",
                "(Ljava/nio/file/Path;Ljava/lang/String;)Ljava/nio/file/DirectoryStream;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newBufferedReader",
                "(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)Ljava/io/BufferedReader;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newBufferedWriter",
                "(Ljava/nio/file/Path;Ljava/nio/charset/Charset;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;", "OS_OPEN_STREAM"));

        // java 8
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newBufferedReader",
                "(Ljava/nio/file/Path;)Ljava/io/BufferedReader;", "OS_OPEN_STREAM"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.nio.file.Files", "newBufferedWriter",
                "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;", "OS_OPEN_STREAM"));

        // Ignore socket input and output streams
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.net.Socket", "getInputStream",
                "()Ljava/io/InputStream;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.net.Socket", "getOutputStream",
                "()Ljava/io/OutputStream;"));

        // Ignore servlet streams
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.servlet.ServletRequest", "getInputStream",
                "()Ljavax/servlet/ServletInputStream;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.servlet.ServletRequest", "getReader",
                "()Ljava/io/BufferedReader;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.servlet.ServletResponse", "getOutputStream",
                "()Ljavax/servlet/ServletOutputStream;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.servlet.ServletResponse", "getWriter",
                "()Ljava/io/PrintWriter;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("jakarta.servlet.ServletRequest", "getInputStream",
                "()Ljakarta/servlet/ServletInputStream;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("jakarta.servlet.ServletRequest", "getReader",
                "()Ljava/io/BufferedReader;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("jakarta.servlet.ServletResponse", "getOutputStream",
                "()Ljakarta/servlet/ServletOutputStream;"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("jakarta.servlet.ServletResponse", "getWriter",
                "()Ljava/io/PrintWriter;"));

        // Ignore System.{in,out,err}
        streamFactoryCollection.add(new StaticFieldLoadStreamFactory("java.io.InputStream", "java.lang.System", "in",
                "Ljava/io/InputStream;"));
        streamFactoryCollection.add(new StaticFieldLoadStreamFactory("java.io.OutputStream", "java.lang.System", "out",
                "Ljava/io/PrintStream;"));
        streamFactoryCollection.add(new StaticFieldLoadStreamFactory("java.io.OutputStream", "java.lang.System", "err",
                "Ljava/io/PrintStream;"));

        // Ignore input streams loaded from instance fields
        streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.InputStream"));
        streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.Reader"));

        // Ignore output streams loaded from instance fields.
        // FIXME: what we really should do here is ignore the stream
        // loaded from the field, but report any streams that wrap
        // it. This is an important and useful distinction that the
        // detector currently doesn't handle. Should be fairly
        // easy to add.
        streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.OutputStream"));
        streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.Writer"));

        // JDBC objects
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));

        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareCall",
                "(Ljava/lang/String;)Ljava/sql/CallableStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareCall",
                "(Ljava/lang/String;II)Ljava/sql/CallableStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "prepareCall",
                "(Ljava/lang/String;III)Ljava/sql/CallableStatement;", "ODR_OPEN_DATABASE_RESOURCE"));

        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.DriverManager", "getConnection",
                "(Ljava/lang/String;)Ljava/sql/Connection;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.DriverManager", "getConnection",
                "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.DriverManager", "getConnection",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.sql.DataSource", "getConnection",
                "()Ljava/sql/Connection;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.sql.DataSource", "getConnection",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;", "ODR_OPEN_DATABASE_RESOURCE"));

        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "()Ljava/sql/Statement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(II)Ljava/sql/Statement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(III)Ljava/sql/Statement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(Ljava/lang/String;)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));
        streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection", "createStatement",
                "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;", "ODR_OPEN_DATABASE_RESOURCE"));

        streamFactoryList = streamFactoryCollection.toArray(new StreamFactory[0]);
    }

    /*
     * ----------------------------------------------------------------------
     * Helper classes
     * ----------------------------------------------------------------------
     */

    private static class PotentialOpenStream {
        public final String bugType;

        public final int priority;

        public final Stream stream;

        @Override
        public String toString() {
            return stream.toString();
        }

        public PotentialOpenStream(String bugType, int priority, Stream stream) {
            this.bugType = bugType;
            this.priority = priority;
            this.stream = stream;
        }
    }

    /*
     * ----------------------------------------------------------------------
     * Fields
     * ----------------------------------------------------------------------
     */

    private final List<PotentialOpenStream> potentialOpenStreamList;

    /*
     * ----------------------------------------------------------------------
     * Implementation
     * ----------------------------------------------------------------------
     */

    public FindOpenStream(BugReporter bugReporter) {
        super(bugReporter);
        this.potentialOpenStreamList = new LinkedList<>();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    // List of words that must appear in names of classes which
    // create possible resources to be tracked. If we don't see a
    // class containing one of these words, then we don't run the
    // detector on the class.
    private static final String[] PRESCREEN_CLASS_LIST = { "Stream", "Reader", "Writer", "ZipFile", "JarFile", "DriverManager",
        "Connection", "Statement", "Files" };

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba
     * .ClassContext)
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        ConstantPool cp = classContext.getJavaClass().getConstantPool();

        // Check to see if the class references any other classes
        // which could be resources we want to track.
        // If we don't find any such classes, we skip analyzing
        // the class. (Note: could do this by method.)
        boolean sawResourceClass = false;
        for (int i = 1; i < cp.getLength(); ++i) {
            Constant constant = cp.getConstant(i);
            // Quote from the JVM specification: "All eight byte constants take up two spots in the constant pool.
            // If this is the n'th byte in the constant pool, then the next item will be numbered n+2"
            // So the indices after CONSTANT_Double and CONSTANT_Long are null, not used and throw ClassFormatException
            if (constant.getTag() == Const.CONSTANT_Double || constant.getTag() == Const.CONSTANT_Long) {
                i++;
            }
            String className = null;
            if (constant instanceof ConstantMethodref) {
                ConstantMethodref cmr = (ConstantMethodref) constant;

                int classIndex = cmr.getClassIndex();
                className = cp.getConstantString(classIndex, Const.CONSTANT_Class);
            } else if (constant instanceof ConstantInterfaceMethodref) {
                ConstantInterfaceMethodref cmr = (ConstantInterfaceMethodref) constant;

                int classIndex = cmr.getClassIndex();
                className = cp.getConstantString(classIndex, Const.CONSTANT_Class);
            }

            if (className != null) {
                if (DEBUG) {
                    System.out.println("FindOpenStream: saw class " + className);
                }

                for (String aPRESCREEN_CLASS_LIST : PRESCREEN_CLASS_LIST) {
                    if (className.indexOf(aPRESCREEN_CLASS_LIST) >= 0) {
                        sawResourceClass = true;
                        break;
                    }
                }
            }

        }

        if (sawResourceClass) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public boolean prescreen(ClassContext classContext, Method method, boolean mightClose) {
        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        if (bytecodeSet == null) {
            return false;
        }
        return bytecodeSet.get(Const.NEW) || bytecodeSet.get(Const.INVOKEINTERFACE)
                || bytecodeSet.get(Const.INVOKESPECIAL) || bytecodeSet.get(Const.INVOKESTATIC)
                || bytecodeSet.get(Const.INVOKEVIRTUAL);
    }

    @Override
    public StreamResourceTracker getResourceTracker(ClassContext classContext, Method method) {
        return new StreamResourceTracker(streamFactoryList, bugReporter);
    }

    public static boolean isMainMethod(Method method) {
        return method.isStatic() && "main".equals(method.getName()) && "([Ljava/lang/String;)V".equals(method.getSignature());
    }

    @Override
    public void analyzeMethod(ClassContext classContext, Method method, StreamResourceTracker resourceTracker,
            ResourceCollection<Stream> resourceCollection) throws CFGBuilderException, DataflowAnalysisException {

        potentialOpenStreamList.clear();

        JavaClass javaClass = classContext.getJavaClass();
        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }
        CFG cfg = classContext.getCFG(method);

        // Add Streams passed into the method as parameters.
        // These are uninteresting, and should poison
        // any streams which wrap them.
        try {
            Type[] parameterTypeList = Type.getArgumentTypes(methodGen.getSignature());
            Location firstLocation = new Location(cfg.getEntry().getFirstInstruction(), cfg.getEntry());

            int local = methodGen.isStatic() ? 0 : 1;

            for (Type type : parameterTypeList) {
                if (type instanceof ObjectType) {
                    ObjectType objectType = (ObjectType) type;

                    for (ObjectType streamBase : streamBaseList) {
                        if (Hierarchy.isSubtype(objectType, streamBase)) {
                            // OK, found a parameter that is a resource.
                            // Create a Stream object to represent it.
                            // The Stream will be uninteresting, so it will
                            // inhibit reporting for any stream that wraps it.
                            Stream paramStream = new Stream(firstLocation, objectType.getClassName(), streamBase.getClassName());
                            paramStream.setIsOpenOnCreation(true);
                            paramStream.setOpenLocation(firstLocation);
                            paramStream.setInstanceParam(local);
                            resourceCollection.addPreexistingResource(paramStream);

                            break;
                        }
                    }
                }

                switch (type.getType()) {
                case Const.T_LONG:
                case Const.T_DOUBLE:
                    local += 2;
                    break;
                default:
                    local += 1;
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }

        // Set precomputed map of Locations to Stream creation points.
        // That way, the StreamResourceTracker won't have to
        // repeatedly try to figure out where Streams are created.
        resourceTracker.setResourceCollection(resourceCollection);

        super.analyzeMethod(classContext, method, resourceTracker, resourceCollection);

        // Compute streams that escape into other streams:
        // this takes wrapper streams into account.
        // This will also compute equivalence classes of streams,
        // so that if one stream in a class is closed,
        // they are all considered closed.
        // (FIXME: this is too simplistic, especially if buffering
        // is involved. Sometime we should really think harder
        // about how this should work.)
        resourceTracker.markTransitiveUninterestingStreamEscapes();

        // For each stream closed on all paths, mark its equivalence
        // class as being closed.
        for (Iterator<Stream> i = resourceCollection.resourceIterator(); i.hasNext();) {
            Stream stream = i.next();
            StreamEquivalenceClass equivalenceClass = resourceTracker.getStreamEquivalenceClass(stream);
            if (stream.isClosed()) {
                equivalenceClass.setClosed();
            }
        }

        // Iterate through potential open streams, reporting warnings
        // for the "interesting" streams that haven't been closed
        // (and aren't in an equivalence class with another stream
        // that was closed).
        for (PotentialOpenStream pos : potentialOpenStreamList) {
            Stream stream = pos.stream;
            if (stream.isClosed()) {
                // Stream was in an equivalence class with another
                // stream that was properly closed.
                continue;
            }

            if (stream.isUninteresting()) {
                continue;
            }

            Location openLocation = stream.getOpenLocation();
            if (openLocation == null) {
                continue;
            }

            if (IGNORE_WRAPPED_UNINTERESTING_STREAMS && resourceTracker.isUninterestingStreamEscape(stream)) {
                continue;
            }

            String sourceFile = javaClass.getSourceFileName();
            String leakClass = stream.getStreamBase();
            if (isMainMethod(method) && (leakClass.contains("InputStream") || leakClass.contains("Reader"))) {
                return;
            }

            bugAccumulator.accumulateBug(new BugInstance(this, pos.bugType, pos.priority)
                    .addClassAndMethod(methodGen, sourceFile).addTypeOfNamedClass(leakClass)
                    .describe(TypeAnnotation.CLOSEIT_ROLE), SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                            sourceFile, stream.getLocation().getHandle()));
        }
    }

    @Override
    public void inspectResult(ClassContext classContext, MethodGen methodGen, CFG cfg,
            Dataflow<ResourceValueFrame, ResourceValueAnalysis<Stream>> dataflow, Stream stream) {

        if (DEBUG) {
            System.out.printf("Result for %s in %s%n", stream, methodGen);
            dataflow.dumpDataflow(dataflow.getAnalysis());

        }
        ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());

        int exitStatus = exitFrame.getStatus();
        if (exitStatus == ResourceValueFrame.OPEN || exitStatus == ResourceValueFrame.OPEN_ON_EXCEPTION_PATH) {

            // FIXME: Stream object should be queried for the
            // priority.

            String bugType = stream.getBugType();
            int priority = NORMAL_PRIORITY;
            if (exitStatus == ResourceValueFrame.OPEN_ON_EXCEPTION_PATH) {
                bugType += "_EXCEPTION_PATH";
                priority = LOW_PRIORITY;
            }

            potentialOpenStreamList.add(new PotentialOpenStream(bugType, priority, stream));
        } else if (exitStatus == ResourceValueFrame.CLOSED) {
            // Remember that this stream was closed on all paths.
            // Later, we will mark all of the streams in its equivalence class
            // as having been closed.
            stream.setClosed();
        }
    }

    // public static void main(String[] argv) throws Exception {
    // if (argv.length != 3) {
    // System.err.println("Usage: " + FindOpenStream.class.getName() +
    // " <class file> <method name> <bytecode offset>");
    // System.exit(1);
    // }
    //
    // String classFile = argv[0];
    // String methodName = argv[1];
    // int offset = Integer.parseInt(argv[2]);
    //
    // ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker> driver =
    // new ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker>() {
    // @Override
    // public StreamResourceTracker createResourceTracker(ClassContext
    // classContext, Method method) {
    // return new StreamResourceTracker(streamFactoryList,
    // classContext.getLookupFailureCallback());
    // }
    // };
    //
    // driver.execute(classFile, methodName, offset);
    // }

}
