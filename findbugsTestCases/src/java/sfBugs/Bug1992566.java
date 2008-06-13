package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;
import java.io.File;
import java.io.IOException;

public class Bug1992566 {
	@NoWarning("RV")
	public void rvFalsePos(File directory) {
		boolean created = directory.mkdirs();
		if (!created) {
			throw new IllegalStateException("directory = " + directory.getPath() +
				" failed to exist and could not be created");
		}
	}
	
	@NoWarning("RV")
	public void rvFalsePos2(File file) throws IOException {
		if ( !file.createNewFile() ) {
			throw new IOException("failed to create " + file.getPath());
		}
	}
}
