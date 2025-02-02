package commonResources;

public class SafeFieldUsages {

    private final Vehicle vehicle = new Vehicle();
    private final Vehicle vehicle2 = new Vehicle();

    public SafeFieldUsages() {
        Thread t1 = new Thread(() -> vehicle.setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private void createVehicle() {
        vehicle2.setModel("Honda");
        vehicle2.setYear(2020);
    }
}
