import java.io.Serializable;


public class BadReadResolve implements Serializable {

	private static final long serialVersionUID = 1L;

	public BadReadResolve readResolve() {
		return new BadReadResolve();
	}

}
