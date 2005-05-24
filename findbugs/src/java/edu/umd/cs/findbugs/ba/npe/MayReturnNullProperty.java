package edu.umd.cs.findbugs.ba.npe;

public class MayReturnNullProperty {
	
	private boolean mayReturnNull;
	
	public MayReturnNullProperty(boolean mayReturnNull) {
		this.mayReturnNull = mayReturnNull;
	}
	
	public boolean mayReturnNull() {
		return mayReturnNull;
	}

}
