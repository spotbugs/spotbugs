package bugIdeas;

public class Ideas_2009_02_25 {

    volatile boolean ready;

    public void setReady() {
        ready = true;
    }

    public void sleepWithExponentalBackoff() throws InterruptedException {
        int delay = 1;
        while (!ready) {
            Thread.sleep(delay);
            delay *= 2;
        }
    }
}
