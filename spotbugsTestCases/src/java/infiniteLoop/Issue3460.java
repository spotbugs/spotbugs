package infiniteLoop;

public class Issue3460 {

    public void loop() {
    	int counter = 0;
    	while (true) {
    		System.out.println("Counter: " + counter);
    		counter++;
    	}
    }
}
