class SpinWait {
	boolean flag;
	volatile boolean vflag;

	void waitForTrue() {
		while(flag);
		}
	void waitForVolatileTrue() {
		while(vflag);
		}
	}
		
	
