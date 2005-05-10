package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.ba.interproc.MethodProperty;

public class MayReturnNullProperty implements MethodProperty<MayReturnNullProperty> {
	
	private static final MayReturnNullProperty doesReturnNullInstance = new MayReturnNullProperty(true);
	private static final MayReturnNullProperty doesNotReturnNullInstance = new MayReturnNullProperty(false);
	
	private boolean mayReturnNull;
	
	public static MayReturnNullProperty doesReturnNullInstance() {
		return doesReturnNullInstance;
	}
	
	public static MayReturnNullProperty doesNotReturnNullInstance() {
		return doesNotReturnNullInstance;
	}
	
	private MayReturnNullProperty(boolean mayReturnNull) {
		this.mayReturnNull = mayReturnNull;
	}
	
	public boolean mayReturnNull() {
		return mayReturnNull;
	}

	public MayReturnNullProperty duplicate() {
		return this;
	}

}
