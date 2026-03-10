package ghIssues;

public abstract class Issue3617 {

	private static final int IDLE = 0;
	private static final int REQUIRED = 1;
	private static final int PROCESSING_TO_IDLE = 2;
	private static final int PROCESSING_TO_REQUIRED = 3;

	void scheduleAfterWrite() {
		int drainStatus = drainStatusOpaque();
		for (;;) {
			switch (drainStatus) {
			case IDLE:
				casDrainStatus(IDLE, REQUIRED);
				scheduleDrainBuffers();
				return;
			case REQUIRED:
				scheduleDrainBuffers();
				return;
			case PROCESSING_TO_IDLE:
				if (casDrainStatus(PROCESSING_TO_IDLE, PROCESSING_TO_REQUIRED)) {
					return;
				}
				drainStatus = drainStatusOpaque();
				continue;
			case PROCESSING_TO_REQUIRED:
				return;
			default:
				throw new IllegalStateException("Invalid drain status: " + drainStatus);
			}
		}
	}

	public void scheduleDrainBuffers() {
	}

	public boolean casDrainStatus(int idle2, int required2) {
		return false;
	}

	public int drainStatusOpaque() {
		return 0;
	}
}
