package commonResources;

public class UnsafeFieldUsage2 {

    private Vehicle vehicle = new Vehicle();

    public UnsafeFieldUsage2() {
        Thread t1 = new Thread(() -> vehicle.setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private void createVehicle() {
        vehicle = new Vehicle();
    }
}
