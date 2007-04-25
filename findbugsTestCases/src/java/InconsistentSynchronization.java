
public class InconsistentSynchronization {
	int x;

	public synchronized int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}


	public synchronized void incrementX() {
		x++;
	}

}
