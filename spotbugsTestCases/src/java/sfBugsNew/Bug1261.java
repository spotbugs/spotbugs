package sfBugsNew;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1261 {
    
    @DesireNoWarning("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public String falsePositive (ServletContext servletContext) throws IOException  {
        try (InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if (inputStream == null) {
                return "#InDevelopment#";
            }
            return new Manifest(inputStream).getMainAttributes().getValue("Implementation-Version");
        }
    }
}
