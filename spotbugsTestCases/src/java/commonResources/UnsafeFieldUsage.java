package commonResources;

public class UnsafeFieldUsage {

    private final Vehicle vehicle = new Vehicle();

    public UnsafeFieldUsage() {
        Thread t1 = new Thread(() -> vehicle.setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private void createVehicle() {
        vehicle.setModel("Honda");
        vehicle.setYear(2020);
        vehicle.setColor("Red");
        vehicle.setPrice(20000);
    }
}
