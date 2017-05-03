package tigerTraps;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bleep {
    String name = "Bleep";

    void setName(String name) {
        this.name = name;
    }

    void backgroundSetName() throws Exception {
        Thread t = new Thread() {
            @Override
            @ExpectWarning("IA")
            public void run() {
                setName("Blat");
            }
        };
        t.start();
        t.join();
        System.out.println(name);
    }

    public static void main(String[] args) throws Exception {
        new Bleep().backgroundSetName();
    }
}
