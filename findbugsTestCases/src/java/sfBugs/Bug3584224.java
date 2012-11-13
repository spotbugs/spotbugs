package sfBugs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Closeables;

import edu.umd.cs.findbugs.annotations.NoWarning;

@NoWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
public class Bug3584224 {

    final static String PROPERTIES_FILENAME = "properies.txt";

    final static Logger LOGGER = Logger.getLogger(Bug3584224.class.getSimpleName());

    public Properties test(Properties props, InputStream stream) {
        try {
            if (stream == null) {
                LOGGER.log(Level.WARNING, String.format("File [%s] not found. Using default properties.", PROPERTIES_FILENAME));
            } else {
                props.load(stream);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, String.format("Error reading [%s]. Using default properties.", PROPERTIES_FILENAME), e);
        } finally {
            Closeables.closeQuietly(stream);
        }
        return props;
    }

}
