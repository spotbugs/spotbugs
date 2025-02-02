package commonResources;

public class SafeFieldGetterUsage {

    private final Vehicle vehicle = new Vehicle();

    public SafeFieldGetterUsage() {
        Thread t1 = new Thread(vehicle::getModel);
        t1.start();

        Thread t2 = new Thread(this::getVehicleInfo);
        t2.start();
    }

    private void getVehicleInfo() {
        vehicle.getModel();
        vehicle.getYear();
    }
}
