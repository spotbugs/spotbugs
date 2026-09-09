package commonResources;

public class UnsafeFieldUsage3 {

    private Vehicle vehicle = new Vehicle();

    private void createVehicle() {
        vehicle = new Vehicle();
    }

    public UnsafeFieldUsage3() {
        Thread t1 = new Thread(() -> vehicle.setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::runThread);
        t2.start();
    }

    private void runThread() {
        createVehicle();
    }
}
