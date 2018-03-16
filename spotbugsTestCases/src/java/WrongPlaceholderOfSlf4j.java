import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrongPlaceholderOfSlf4j {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(WrongPlaceholderOfSlf4j.class);

        String strParam = logger.getName();
        String[] arrayParam = new String[] { "1", "2", "3" };

        /* Report a bug, have 2 placeholders but need 0 */
        logger.warn("hello {} {}");

        /* Report a bug, have 2 placeholders but need 1 */
        logger.info("hello {} {}", strParam);

        /*
         * Report a bug, have 2 placeholders but need 0; the last 'Throwable' does not
         * need a placeholder
         */
        logger.error("hello {} {}", new NullPointerException());

        /* No bug */
        logger.info("hello {} {}", strParam, strParam);

        /* Report a bug, have 2 placeholders but need 1 */
        logger.debug("hello {} {}", strParam, new NullPointerException());

        /* No bug */
        logger.info("hello {}", strParam, new Throwable());

        /* Report a bug, have 2 placeholders but need 3 */
        logger.trace("hello {} {}", strParam, strParam, strParam);

        /* No bug */
        logger.info("hello {} {}", strParam, strParam, new NullPointerException());

        /* Report a bug, have 2 placeholders but need 4 */
        logger.info("hello {} {}", strParam, strParam, strParam, strParam);

        /* Report a bug, have 2 placeholders but need 3 */
        logger.info("hello {} {}", strParam, strParam, strParam, new NullPointerException());

        /* Report a bug, have 4 placeholders but need 5 */
        logger.info("hello {} {} {} {}", strParam, strParam, strParam, strParam, arrayParam,
                new NullPointerException());
    }
}