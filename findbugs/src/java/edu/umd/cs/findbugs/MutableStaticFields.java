package edu.umd.cs.findbugs;
import java.util.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class MutableStaticFields extends BytecodeScanningDetector implements   Constants2 {


  static String extractPackage(String c) {
	int i = c.lastIndexOf('/');
	if (i < 0) return "";
	return c.substring(0,i);
	}
  static boolean mutableSignature(String sig) {
	return sig.equals("Ljava/util/Hashtable;")
		 || sig.charAt(0) == '[';
	}
   static class FieldRecord {
	String className;
	String name;
	String signature;
	boolean isPublic;
	boolean isFinal;
	}


	LinkedList<FieldRecord> seen = new LinkedList<FieldRecord>();	
	boolean publicClass;
	boolean zeroOnTOS;
	boolean emptyArrayOnTOS;
	boolean inStaticInitializer;
	List problems = new LinkedList();
	String packageName;
	HashSet<String> unsafeValue = new HashSet<String>();
	HashSet<String> interfaces = new HashSet<String>();
	HashSet<String> notFinal = new HashSet<String>();
	HashSet<String> outsidePackage = new HashSet<String>();
	HashSet allocatesZeroLengthArray = new HashSet();
	private BugReporter bugReporter;

    public MutableStaticFields(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(JavaClass obj) { 
	super.visit(obj);
	int flags = obj.getAccessFlags();
	publicClass = (flags & ACC_PUBLIC) != 0 
				&& !betterClassName.startsWith("sun.");
	if ((flags & ACC_INTERFACE) != 0)
		interfaces.add(betterClassName);

	packageName = extractPackage(className);
	}
    public void visit(Method obj) {
	zeroOnTOS = false;
	// System.out.println(methodName);
	inStaticInitializer = methodName.equals("<clinit>");
	}
    public void sawOpcode(int seen) {
	// System.out.println("saw	"	+ OPCODE_NAMES[seen] + "	" + zeroOnTOS);
	switch (seen) {
		case GETSTATIC:
		case PUTSTATIC:
			String packageConstant = extractPackage(classConstant);
			boolean samePackage =
			   packageName.equals(extractPackage(classConstant));
			boolean initOnly =
			   seen == GETSTATIC ||
			   className.equals(classConstant)
			   && inStaticInitializer;
			boolean safeValue =
			   seen == GETSTATIC || emptyArrayOnTOS
				|| !mutableSignature(sigConstant);
			String name = (classConstant + "." + nameConstant)
                                        .replace('/','.');
			/*
			System.out.println("In " + betterClassName
				+ " accessing " 
				+ (classConstant + "." + nameConstant)
				+ "	" + samePackage
				+ "	" + initOnly
				+ "	" + safeValue
				);
			*/

			if (!samePackage) 
				outsidePackage.add(name);
				
			if (!initOnly) 
				notFinal.add(name);
				
			if (!safeValue) 
				unsafeValue.add(name);
		 	break;
		case ANEWARRAY:
		case NEWARRAY:
		    if (zeroOnTOS)
			emptyArrayOnTOS = true;
		    zeroOnTOS = false;
		    return;
		case ICONST_0:
			zeroOnTOS = true;
			emptyArrayOnTOS = false;
			return;
		}
	zeroOnTOS = false;
	emptyArrayOnTOS = false;
	}
    public void visit(Field obj) {
        super.visit(obj);
	int flags = obj.getAccessFlags();
	boolean isStatic = (flags & ACC_STATIC) != 0;
	if (!isStatic) return;
	boolean isFinal = (flags & ACC_FINAL) != 0;
	boolean isPublic = publicClass && (flags & ACC_PUBLIC) != 0;
	boolean isProtected = publicClass && (flags & ACC_PROTECTED) != 0;
	if (!isPublic && !isProtected) return;

	boolean isHashtable = fieldSig.equals("Ljava/util/Hashtable;");
	boolean isArray = fieldSig.charAt(0) == '[';

	if (isFinal && !(isHashtable || isArray)) return;

	FieldRecord f = new FieldRecord();
	f.className = betterClassName;
	f.name = fieldName;
	f.signature = betterFieldSig;
	f.isPublic = isPublic;
	f.isFinal = isFinal;

	seen.add(f);
	
	}
 public void report() {
	/*
	for(Iterator i = unsafeValue.iterator(); i.hasNext(); ) {
		System.out.println("Unsafe: " + i.next());
		}
	*/
	for(Iterator<FieldRecord> i = seen.iterator(); i.hasNext(); ) {
	  FieldRecord f = i.next();
	  boolean isFinal = f.isFinal;
	  String className = f.className;
	  String fieldSig = f.signature;
	  String fieldName = f.name;
	  String name = className + "." + fieldName;
	  boolean couldBeFinal = !isFinal
				&& !notFinal.contains(name);
	  boolean isPublic = f.isPublic;
	  boolean couldBePackage = !outsidePackage.contains(name);
	  boolean movedOutofInterface = couldBePackage &&
			interfaces.contains(className);
	  boolean isHashtable = fieldSig.equals("Ljava/util/Hashtable;");
	  boolean isArray = fieldSig.charAt(0) == '['
		&& unsafeValue.contains(name);
	  /*
	  System.out.println(className + "."  + fieldName
				+ " : " + fieldSig
			+ "	" + isHashtable
			+ "	" + isArray
				);
	*/
	String bugType;
	if (isFinal && !isHashtable && !isArray) {
		// System.out.println( header + " is a safe zero length array");
		return;
	} else if (movedOutofInterface && couldBeFinal) {
		bugType = "MS_FINAL_OOI_PKGPROTECT";
	} else if (couldBePackage && couldBeFinal && (isHashtable || isArray)) 
		bugType = "MS_FINAL_PKGPROTECT";
	else if (couldBeFinal && !isHashtable && !isArray)
		bugType = "MS_SHOULD_BE_FINAL";
	else if (movedOutofInterface)
		bugType = "MS_OOI_PKGPROTECT";
	else if (couldBePackage)
		bugType = "MS_PKGPROTECT";
	else if (isHashtable) 
		bugType = "MS_MUTABLE_HASHTABLE";
	else if (isArray) 
		bugType = "MS_MUTABLE_ARRAY";
	else if (!isFinal) 
		bugType = "MS_CANNOT_BE_FINAL";
	else throw new RuntimeException("impossible");

	bugReporter.reportBug(new BugInstance(bugType, NORMAL_PRIORITY)
		.addClass(className)
		.addField(className, f.name, f.signature, true));

	}
	}
}
