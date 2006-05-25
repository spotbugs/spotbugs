package jcipAnnotations;

import net.jcip.annotations.GuardedBy;
public class TestGuardedBy {
	
	@GuardedBy("this")
	int x;
	int y;
	int z;
	
	public void setX(int v) {
		x = v;
	}
	public void setY(int v) {
		y = v;
	}
	public synchronized void setZ(int v) {
		z = v;
	}
	public  int getXY() {
		return x+y;
	}
	public synchronized int getYZ() {
		return y+z;
	}
}
