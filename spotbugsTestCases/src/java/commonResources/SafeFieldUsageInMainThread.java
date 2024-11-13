package commonResources;

public class SafeFieldUsageInMainThread {

    private Vehicle vehicle = new Vehicle();

    public SafeFieldUsageInMainThread() {
        Thread thread = new Thread(this::createVehicle);
        thread.start();

        vehicle.setModel("Toyota");
    }

    private void createVehicle() {
        vehicle = new Vehicle();
    }
}
