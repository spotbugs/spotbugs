package uselessThread;

class UsefulThread extends Thread {
	public UsefulThread() {
		super("Usefull-" + System.currentTimeMillis());
	}

	@Override
    public void run() {
		System.out.println("I am a useful thread!");
	}
}
