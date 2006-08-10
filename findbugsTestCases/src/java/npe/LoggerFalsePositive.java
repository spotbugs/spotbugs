package npe;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerFalsePositive {
	public LoggerFalsePositive(String container) {
		this.container = container;
	}
	private Logger getLogger() {
		return Logger.getAnonymousLogger();
	}

	String container;

	public void log(String message) {

		Logger logger = null;
		if (container != null)
			logger = getLogger();
		if (logger != null)
			logger.log(Level.SEVERE, "StandardWrapperValve["
					+ container.toLowerCase() + "]: " + message);
		else {
			String containerName = null;
			if (container != null)
				containerName = container.toLowerCase();
			System.out.println("StandardWrapperValve[" + containerName + "]: "
					+ message);
		}

	}
}
