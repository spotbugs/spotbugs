package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.Constants2;

public class SerializableIdiom extends BetterVisitor implements Detector, Constants2 {


    boolean sawSerialVersionUID;
    boolean isSerializable;
    int synchronizedMethods;
    boolean writeObjectIsSynchronized;
    private BugReporter bugReporter;

    public SerializableIdiom(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

    public void report() { }

    public void visitJavaClass(JavaClass obj)     {      
sawSerialVersionUID = false;
       super.visitJavaClass(obj);
        constant_pool.accept(this);
    		isSerializable = false;
		String [] interface_names = obj.getInterfaceNames();
		for(int i=0; i < interface_names.length; i++) {
			if (interface_names[i].equals("java.io.Serializable")) {
				isSerializable = true;
			}
		}
        Field[] fields = obj.getFields();
        for(int i = 0; i < fields.length; i++) fields[i].accept(this);
	if (isSerializable && !sawSerialVersionUID)
//		bugReporter.reportBug(BugInstance.inClass("SE_NO_SERIALVERSIONID", UNKNOWN_PRIORITY, this));
		bugReporter.reportBug(new BugInstance("SE_NO_SERIALVERSIONID", NORMAL_PRIORITY).addClass(this));

	synchronizedMethods = 0;
	writeObjectIsSynchronized = false;
        Method[] methods = obj.getMethods();
        for(int i = 0; i < methods.length; i++) methods[i].accept(this);
	if (writeObjectIsSynchronized && synchronizedMethods == 0)
//		bugReporter.reportBug(BugInstance.inClass("WS_WRITEOBJECT_SYNC", UNKNOWN_PRIORITY, this));
		bugReporter.reportBug(new BugInstance("WS_WRITEOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
        }

    public void visit(Method obj) {
	int accessFlags = obj.getAccessFlags();
        boolean isSynchronized = (accessFlags & ACC_SYNCHRONIZED) != 0;
	// System.out.println(methodName + isSynchronized);
	if (!isSynchronized) return;
	if (methodName.equals("readObject")) 
//		bugReporter.reportBug(BugInstance.inClass("RS_READOBJECT_SYNC", UNKNOWN_PRIORITY, this));
		bugReporter.reportBug(new BugInstance("RS_READOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
	else if (methodName.equals("writeObject")) 
		writeObjectIsSynchronized = true;
	else synchronizedMethods++;

	}
		
	
    public void visit(Field obj) {
	// System.out.println("Saw " + betterClassName + "." + fieldName);
        super.visit(obj);

	if (!fieldName.equals("serialVersionUID")) return;
	// System.out.println("Saw " + betterClassName + "." + fieldName);
	int flags = obj.getAccessFlags();
	if ((flags & ACC_STATIC) == 0) {
//		bugReporter.reportBug(BugInstance.inClass("SE_NONSTATIC_SERIALVERSIONID", UNKNOWN_PRIORITY, this));
		bugReporter.reportBug(new BugInstance("SE_NONSTATIC_SERIALVERSIONID", NORMAL_PRIORITY)
			.addClass(this)
			.addField(FieldAnnotation.fromVisitedField(this)));
		return;
		}
	sawSerialVersionUID = true;
	}


}
