package edu.umd.cs.findbugs.ba.ca;

public class Call {
	private final String className;
	private final String methodName;
	private final String methodSig;
	
	public Call(String className, String methodName, String methodSig) {
		this.className = className;
		this.methodName= methodName;
		this.methodSig = methodSig;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getMethodSig() {
		return methodSig;
	}
	
	//@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		Call other = (Call) obj;
		return this.className.equals(other.className)
			&& this.methodName.equals(other.methodName)
			&& this.methodSig.equals(other.methodSig);
	}
	
	//@Override
	public int hashCode() {
		return className.hashCode() + methodName.hashCode() + methodSig.hashCode();
	}
}
