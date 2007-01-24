import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
public class  EscapeIntoLoadProperties {

	static Properties f(File f) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(f));
		return p;
	}
}
