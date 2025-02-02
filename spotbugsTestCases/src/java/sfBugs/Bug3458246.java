package sfBugs;

import java.util.logging.Logger;

public class Bug3458246 {

    static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    void foo(String frontendRequest, String messageEvent) {

        final String frontEndUrl = frontendRequest.split("/")[2];
        switch (frontEndUrl) {
        case "A":
            processRequest(frontendRequest, messageEvent, "A");
            break;
        case "JSCSS":
            processRequest(frontendRequest, messageEvent, "JSCSS");
            break;
        case "COLUMNISTS":
            processRequest(frontendRequest, messageEvent, "COLUMNISTS");
            break;
        case "CHANNEL_IMAGES":
            processRequest(frontendRequest, messageEvent, "CHANNEL_IMAGES");
            break;
        default:
            LOGGER.warning("Can't match " + frontEndUrl);

            return;
        }
    }

    private void processRequest(String frontendRequest, String messageEvent, String string) {
        // TODO Auto-generated method stub

    }

}
