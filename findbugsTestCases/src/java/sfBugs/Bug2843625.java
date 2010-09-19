package sfBugs;

import java.util.logging.Logger;

public class Bug2843625 {
    public static final Logger log = Logger.getAnonymousLogger();

    public static Logger log2 = Logger.getAnonymousLogger();

    public Logger log3 = Logger.getAnonymousLogger();

    public static final Logger Log = Logger.getAnonymousLogger();

    public static Logger Log2 = Logger.getAnonymousLogger();

    public Logger Log3 = Logger.getAnonymousLogger();

    public static final Logger LOG = Logger.getAnonymousLogger();

    public static Logger LOG2 = Logger.getAnonymousLogger();

    public Logger LOG3 = Logger.getAnonymousLogger();

    public enum ApplicationType {
        Data, data, DATA
    }
}
