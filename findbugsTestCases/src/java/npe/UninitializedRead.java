package npe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class UninitializedRead {
	PrintWriter out;
	UninitializedRead(boolean debug, File f) throws IOException {
		if (debug) {
			// TODO: generate a high priority warning here
			out.println("Debugging started");
		}
		out = new PrintWriter(new FileWriter(f));
	}

}
