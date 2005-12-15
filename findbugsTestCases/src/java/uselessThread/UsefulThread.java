package uselessThread;

class UsefulThread extends Thread {
	public UsefulThread() {
		super("Usefull-" + System.currentTimeMillis());
	}

	public void run() {
		System.out.println("I am a useful thread!");
	}
}
