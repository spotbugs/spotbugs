package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.ba.interproc.MethodProperty;

public class MayReturnNullProperty implements MethodProperty<MayReturnNullProperty> {
	
	private boolean mayReturnNull;
	
	public MayReturnNullProperty(boolean mayReturnNull) {
		this.mayReturnNull = mayReturnNull;
	}
	
	public boolean mayReturnNull() {
		return mayReturnNull;
	}

	public MayReturnNullProperty duplicate() {
		return this;
	}
	
	public void makeSameAs(MayReturnNullProperty other) {
		this.mayReturnNull = other.mayReturnNull;
	}

}
