import java.io.*;
import java.util.*;
public class  EscapeIntoLoadProperties {

	static Properties f(File f) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(f));
		return p;
	}
}
