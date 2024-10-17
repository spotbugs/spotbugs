package commonResources;

public class SynchronizedSafeFieldUsage {

    private final Vehicle vehicle = new Vehicle();

    public SynchronizedSafeFieldUsage() {
        Thread t1 = new Thread(() -> {
            synchronized (vehicle) {
                vehicle.setModel("Toyota");
            }
        });
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private synchronized void createVehicle() {
        vehicle.setModel("Honda");
        vehicle.setYear(2020);
    }
}
