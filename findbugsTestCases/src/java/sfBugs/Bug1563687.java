package sfBugs;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Bug1563687 {

	public void write(String file, String text) throws IOException {
		BufferedOutputStream out = null;
        try {
			 out = new BufferedOutputStream (new FileOutputStream(file));
			 out.write(42);
		} finally {
            if (out != null) out.close();
		}
	}

}
