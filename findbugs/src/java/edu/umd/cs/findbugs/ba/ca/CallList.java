package edu.umd.cs.findbugs.ba.ca;

import java.util.ArrayList;

public class CallList {
	private boolean isTop, isBottom;
	private ArrayList<Call> callList;
	
	public CallList() {
		this.callList = new ArrayList<Call>();
	}
	
	public boolean isTop() {
		return isTop;
	}
	
	public boolean isBottom() {
		return isBottom;
	}
	
	public void setTop() {
		this.isTop = true;
		this.isBottom = false;
		this.callList.clear();
	}
	
	public void setBottom() {
		this.isTop = false;
		this.isBottom = true;
		this.callList.clear();
	}
	
	public void clear() {
		this.isTop = this.isBottom = false;
		this.callList.clear();
	}
	
	public void add(Call call) {
		callList.add(call);
	}
	
	public int size() {
		return callList.size();
	}
	
	public Call get(int index) {
		return callList.get(index);
	}
	
	public void copyFrom(CallList other) {
		this.isTop = other.isTop;
		this.isBottom = other.isBottom;
		this.callList.clear();
		this.callList.addAll(other.callList);
	}
	
	public static CallList merge(CallList a, CallList b) {
		CallList result = new CallList();

		if (a.isBottom || b.isBottom) {
			result.isBottom = true;
		} else if (a.isTop) {
			result.copyFrom(b);
		} else if (b.isTop) {
			result.copyFrom(a);
		} else {
			// Result is the common prefix
			int len = Math.min(a.size(), b.size());
			for (int i = 0; i < len; ++i) {
				if (!a.get(i).equals(b.get(i)))
					break;
				result.add(a.get(i));
			}
		}
		return result;
	}
}
