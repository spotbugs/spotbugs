package tomcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ExtendedProperties extends Properties {
	String file;

	String basePath;

	char fileSeparator = File.separatorChar;

	ExtendedProperties defaults;

	public ExtendedProperties(String file) throws IOException {
		this(file, null);
	}

	public ExtendedProperties(String file, String defaultFile)
			throws IOException {
		this.file = file;

		basePath = new File(file).getAbsolutePath();
		basePath = basePath.substring(0,
				basePath.lastIndexOf(fileSeparator) + 1);

		FileInputStream fileInputStream = new FileInputStream(file);
		// this.load(fileInputStream);

		if (defaultFile != null) {
			defaults = new ExtendedProperties(defaultFile);
		}
	}

}
