package deref;

import java.io.IOException;
import java.io.OutputStream;

public class DereferenceByCalledBuggyMethod {
	
	/** This method is buggy and we should report a NP warning here */
	
	public void closeit(OutputStream out) throws IOException {
		if (out == null) 
			out.close();
	}
	
	/** Nothing wrong with this method */
	public void falsePositive() throws IOException {
		closeit(null);
	}

}
