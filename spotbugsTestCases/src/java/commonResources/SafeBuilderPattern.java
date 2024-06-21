package commonResources;

public class SafeBuilderPattern {

    private volatile Vehicle vehicle = new Vehicle();

    public SafeBuilderPattern() {
        Thread t1 = new Thread(() -> Vehicle.Builder.newInstance().
                setColor("red").setPrice(6000).build());
        t1.start();

        Thread t2 = new Thread(() -> Vehicle.Builder.newInstance().
                setYear(2020).setModel("Tesla").build());
        t2.start();
    }
}
