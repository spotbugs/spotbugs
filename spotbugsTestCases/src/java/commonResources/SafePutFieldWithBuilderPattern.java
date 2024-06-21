package commonResources;

public class SafePutFieldWithBuilderPattern {

    private volatile Vehicle vehicle;

    public SafePutFieldWithBuilderPattern() {
        Thread t1 = new Thread(() -> vehicle = Vehicle.Builder.newInstance().
                setColor("red").setPrice(6000).build());
        t1.start();

        Thread t2 = new Thread(() -> vehicle = Vehicle.Builder.newInstance().
                setYear(2020).setModel("Tesla").build());
        t2.start();
    }
}
