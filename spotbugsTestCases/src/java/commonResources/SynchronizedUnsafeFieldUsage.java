package commonResources;

public class SynchronizedUnsafeFieldUsage {

    private final Vehicle vehicle = new Vehicle();

    public SynchronizedUnsafeFieldUsage() {
        Thread t1 = new Thread(() -> vehicle.setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private synchronized void createVehicle() {
        vehicle.setModel("Honda");
    }
}
