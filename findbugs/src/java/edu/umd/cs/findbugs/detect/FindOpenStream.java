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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A Detector to look for streams that are opened in a method,
 * do not escape the method, and are not closed on all paths
 * out of the method.  Note that "stream" is a bit misleading,
 * since we also use the detector to look for database resources
 * that aren't closed.
 *
 * @author David Hovemeyer
 */
public final class FindOpenStream extends ResourceTrackingDetector<Stream, StreamResourceTracker> implements StatelessDetector {
	static final boolean DEBUG = SystemProperties.getBoolean("fos.debug");
	static final boolean IGNORE_WRAPPED_UNINTERESTING_STREAMS = !SystemProperties.getBoolean("fos.allowWUS");

	/* ----------------------------------------------------------------------
	 * Tracked resource types
	 * ---------------------------------------------------------------------- */

	/**
	 * List of base classes of tracked resources.
	 */
	static final ObjectType[] streamBaseList =
	        {ObjectTypeFactory.getInstance("java.io.InputStream"),
	         ObjectTypeFactory.getInstance("java.io.OutputStream"),
	         ObjectTypeFactory.getInstance("java.io.Reader"),
	         ObjectTypeFactory.getInstance("java.io.Writer"),
	         ObjectTypeFactory.getInstance("java.sql.Connection"),
	         ObjectTypeFactory.getInstance("java.sql.Statement"),
	         ObjectTypeFactory.getInstance("java.sql.ResultSet")};

	/**
	 * StreamFactory objects used to detect resources
	 * created within analyzed methods.
	 */
	static final StreamFactory[] streamFactoryList;

	static {
		ArrayList<StreamFactory> streamFactoryCollection = new ArrayList<StreamFactory>();

		// Examine InputStreams, OutputStreams, Readers, and Writers,
		// ignoring byte array, object stream, char array, and String variants.
		streamFactoryCollection.add(new IOStreamFactory("java.io.InputStream",
		        new String[]{"java.io.ByteArrayInputStream", "java.io.StringBufferInputStream",  "java.io.PipedInputStream"
				,"java.io.ObjectInputStream"
				},
		        "OS_OPEN_STREAM"));
		streamFactoryCollection.add(new IOStreamFactory("java.io.OutputStream",
		        new String[]{"java.io.ByteArrayOutputStream", "java.io.PipedOutputStream"
				 , "java.io.ObjectOutputStream"
				},
		        "OS_OPEN_STREAM"));
		streamFactoryCollection.add(new IOStreamFactory("java.io.Reader",
		        new String[]{"java.io.StringReader", "java.io.CharArrayReader", "java.io.PipedReader"},
		        "OS_OPEN_STREAM"));
		streamFactoryCollection.add(new IOStreamFactory("java.io.Writer",
		        new String[]{"java.io.StringWriter", "java.io.CharArrayWriter", "java.io.PipedWriter"},
		        "OS_OPEN_STREAM"));

		// Ignore socket input and output streams
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.net.Socket",
		        "getInputStream", "()Ljava/io/InputStream;"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.net.Socket",
		        "getOutputStream", "()Ljava/io/OutputStream;"));

		// Ignore System.{in,out,err}
		streamFactoryCollection.add(new StaticFieldLoadStreamFactory("java.io.InputStream",
		        "java.lang.System", "in", "Ljava/io/InputStream;"));
		streamFactoryCollection.add(new StaticFieldLoadStreamFactory("java.io.OutputStream",
		        "java.lang.System", "out", "Ljava/io/PrintStream;"));
		streamFactoryCollection.add(new StaticFieldLoadStreamFactory("java.io.OutputStream",
		        "java.lang.System", "err", "Ljava/io/PrintStream;"));

		// Ignore input streams loaded from instance fields
		streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.InputStream"));
		streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.Reader"));

		// Ignore output streams loaded from instance fields.
		// FIXME: what we really should do here is ignore the stream
		// loaded from the field, but report any streams that wrap
		// it.  This is an important and useful distinction that the
		// detector currently doesn't handle.  Should be fairly
		// easy to add.
		streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.OutputStream"));
		streamFactoryCollection.add(new InstanceFieldLoadStreamFactory("java.io.Writer"));

		// JDBC objects
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareStatement", "(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareStatement", "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareStatement", "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareStatement", "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareStatement", "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareStatement", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));

		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareCall", "(Ljava/lang/String;)Ljava/sql/CallableStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareCall", "(Ljava/lang/String;II)Ljava/sql/CallableStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "prepareCall", "(Ljava/lang/String;III)Ljava/sql/CallableStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));

		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.DriverManager",
		        "getConnection", "(Ljava/lang/String;)Ljava/sql/Connection;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.DriverManager",
		        "getConnection", "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.DriverManager",
		        "getConnection",
		        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.sql.DataSource",
				"getConnection",
				"()Ljava/sql/Connection;",
				"ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("javax.sql.DataSource",
				"getConnection",
				"(Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;",
				"ODR_OPEN_DATABASE_RESOURCE"));

		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "()Ljava/sql/Statement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(II)Ljava/sql/Statement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(III)Ljava/sql/Statement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));
		streamFactoryCollection.add(new MethodReturnValueStreamFactory("java.sql.Connection",
		        "createStatement", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;",
		        "ODR_OPEN_DATABASE_RESOURCE"));

		streamFactoryList = streamFactoryCollection.toArray(new StreamFactory[streamFactoryCollection.size()]);
	}

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static class PotentialOpenStream {
		public final String bugType;
		public final int priority;
		public final Stream stream;

		public PotentialOpenStream(String bugType, int priority, Stream stream) {
			this.bugType = bugType;
			this.priority = priority;
			this.stream = stream;
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private List<PotentialOpenStream> potentialOpenStreamList;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindOpenStream(BugReporter bugReporter) {
		super(bugReporter);
		this.potentialOpenStreamList = new LinkedList<PotentialOpenStream>();
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
	// create possible resources to be tracked.  If we don't see a
	// class containing one of these words, then we don't run the
	// detector on the class.
	private static final String[] PRESCREEN_CLASS_LIST =
		{ "Stream", "Reader", "Writer", "DriverManager", "Connection" }; 

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	@Override
         public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();

		// Check to see if the class references any other classes
		// which could be resources we want to track.
		// If we don't find any such classes, we skip analyzing
		// the class.  (Note: could do this by method.)
		boolean sawResourceClass = false;
		for (int i = 0; i < jclass.getConstantPool().getLength(); ++i) {
			Constant constant = jclass.getConstantPool().getConstant(i);
			
			if (constant instanceof ConstantMethodref) {
				ConstantMethodref cmr = (ConstantMethodref) constant;
				
				int classIndex = cmr.getClassIndex();
				String className = jclass.getConstantPool().getConstantString(
						classIndex, Constants.CONSTANT_Class);
				
				if (DEBUG) System.out.println("FindOpenStream: saw class " + className);
				
				if (className != null) {
					for (String aPRESCREEN_CLASS_LIST : PRESCREEN_CLASS_LIST) {
						if (className.indexOf(aPRESCREEN_CLASS_LIST) >= 0) {
							sawResourceClass = true;
							break;
						}
					}
				}
			}
		}
		
		if (sawResourceClass) {
			super.visitClassContext(classContext);
		}
	}

	@Override
         public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (bytecodeSet == null) return false;
		return bytecodeSet.get(Constants.NEW)
		        || bytecodeSet.get(Constants.INVOKEINTERFACE)
		        || bytecodeSet.get(Constants.INVOKESPECIAL)
		        || bytecodeSet.get(Constants.INVOKESTATIC)
		        || bytecodeSet.get(Constants.INVOKEVIRTUAL);
	}

	@Override
         public StreamResourceTracker getResourceTracker(ClassContext classContext, Method method) {
		return new StreamResourceTracker(streamFactoryList, bugReporter);
	}

	public static boolean isMainMethod(Method method) {
		return method.isStatic()
		        && method.getName().equals("main")
		        && method.getSignature().equals("([Ljava/lang/String;)V");
	}

	@Override
         public void analyzeMethod(ClassContext classContext, Method method,
	                          StreamResourceTracker resourceTracker,
	                          ResourceCollection<Stream> resourceCollection)
	        throws CFGBuilderException, DataflowAnalysisException {

		if (isMainMethod(method)) return;

		potentialOpenStreamList.clear();

		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
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

							Stream paramStream =
									new Stream(firstLocation, objectType.getClassName(), streamBase.getClassName());
							paramStream.setIsOpenOnCreation(true);
							paramStream.setOpenLocation(firstLocation);
							paramStream.setInstanceParam(local);
							resourceCollection.addPreexistingResource(paramStream);

							break;
						}
					}
				}

				switch (type.getType()) {
				case Constants.T_LONG:
				case Constants.T_DOUBLE:
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
		// is involved.  Sometime we should really think harder
		// about how this should work.)
		resourceTracker.markTransitiveUninterestingStreamEscapes();

		// For each stream closed on all paths, mark its equivalence
		// class as being closed.
		for (Iterator<Stream> i = resourceCollection.resourceIterator(); i.hasNext();) {
			Stream stream = i.next();
			StreamEquivalenceClass equivalenceClass = resourceTracker.getStreamEquivalenceClass(stream);
			if (stream.isClosed())
				equivalenceClass.setClosed();
		}

		// Iterate through potential open streams, reporting warnings
		// for the "interesting" streams that haven't been closed
		// (and aren't in an equivalence class with another stream
		// that was closed).
		for (PotentialOpenStream pos : potentialOpenStreamList) {
			Stream stream = pos.stream;
			if (stream.isClosed())
				// Stream was in an equivalence class with another
				// stream that was properly closed.
				continue;

			if (stream.isUninteresting())
				continue;

			Location openLocation = stream.getOpenLocation();
			if (openLocation == null)
				continue;

			if (IGNORE_WRAPPED_UNINTERESTING_STREAMS
					&& resourceTracker.isUninterestingStreamEscape(stream))
				continue;

			String sourceFile = javaClass.getSourceFileName();
			bugReporter.reportBug(new BugInstance(this, pos.bugType, pos.priority)
					.addClassAndMethod(methodGen, sourceFile)
					.addSourceLine(classContext, methodGen, sourceFile, stream.getLocation().getHandle()));
		}
	}

	@Override
         public void inspectResult(ClassContext classContext, MethodGen methodGen, CFG cfg,
	                          Dataflow<ResourceValueFrame, ResourceValueAnalysis<Stream>> dataflow, Stream stream) {
		
		ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());

		int exitStatus = exitFrame.getStatus();
		if (exitStatus == ResourceValueFrame.OPEN
		        || exitStatus == ResourceValueFrame.OPEN_ON_EXCEPTION_PATH) {

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

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + FindOpenStream.class.getName() +
			        " <class file> <method name> <bytecode offset>");
			System.exit(1);
		}

		String classFile = argv[0];
		String methodName = argv[1];
		int offset = Integer.parseInt(argv[2]);

		ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker> driver =
		        new ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker>() {
			        @Override
                                 public StreamResourceTracker createResourceTracker(ClassContext classContext, Method method) {
				        return new StreamResourceTracker(streamFactoryList, classContext.getLookupFailureCallback());
			        }
		        };

		driver.execute(classFile, methodName, offset);
	}

}

// vim:ts=3
