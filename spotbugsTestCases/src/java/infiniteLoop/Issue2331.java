package infiniteLoop;

public class Issue2331 {

    public void loop() {
        boolean b = true;
        do {
            System.out.println("Wow");
        } while (b);
    }
}
