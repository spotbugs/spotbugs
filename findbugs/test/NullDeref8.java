import java.io.*;

class NullDeref8 {

  public int foo(String filename) throws IOException {
	InputStream in = null;
	try {
		in = new FileInputStream(filename);
	}
	finally {
		if (in == null) 
			System.out.println("Failure");
		}
	return in.read();
	}
  }
