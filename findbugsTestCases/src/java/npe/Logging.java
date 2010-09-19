package npe;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {
    private static Logger logger = Logger.getLogger("npe");

    int f(Object x) {
        if (x == null)
            logger.log(Level.SEVERE, "x is null");
        return x.hashCode();
    }

}
