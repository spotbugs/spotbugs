package sfBugs;

public class Bug1460277 {
	static Object DOMAINCFG = new Object();
	void test() throws InterruptedException {
		synchronized(DOMAINCFG) {
            DOMAINCFG.wait();
		}
	}

}
