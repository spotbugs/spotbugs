package sfBugs;

import java.util.logging.Level;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class NBug1165 {
    private static java.util.logging.Logger jerseyLogger = java.util.logging.Logger.getLogger("com.sun.jersey");

    @NoWarning("LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE")
    public static void main(String[] args) throws Throwable {
        java.util.logging.Handler[] handlers = java.util.logging.Logger.getLogger("").getHandlers();
        for (java.util.logging.Handler handler : handlers) {
            handler.setLevel(Level.ALL);
        }
        jerseyLogger.setLevel(Level.ALL);
    }

}
