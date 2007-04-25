package sfBugs;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;

public class Bug1578441 {
	@In Object x;
	@Out Object y;

	@Override
	public int hashCode() {
		return x.hashCode();
	}

	public void setY(Object y) {
		this.y = y;
	}


}
