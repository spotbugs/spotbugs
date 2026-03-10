package commonResources;

public class UnsafeFieldUsage4 {

    private Vehicle vehicle = new Vehicle();

    private void runThread() {
        createVehicle();
    }

    private void createVehicle() {
        vehicle = new Vehicle();
    }

    public UnsafeFieldUsage4() {
        Thread t1 = new Thread(() -> vehicle.setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::runThread);
        t2.start();
    }
}
