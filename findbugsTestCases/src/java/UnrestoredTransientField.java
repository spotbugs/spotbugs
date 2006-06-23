import java.io.Serializable;


public class UnrestoredTransientField implements Serializable {
	
	private static final long serialVersionUID = 1L;
	transient int x = 0;
	
	public void inc() {
		x++;
	}
	public void add(int v) {
		x+=v;
	}
	public int getValue() {
		return x;
	}

}
