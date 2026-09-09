package commonResources;

import java.util.concurrent.atomic.AtomicReference;

public class SafeAtomicFieldUsage {

    private final AtomicReference<Vehicle> vehicle = new AtomicReference<>(new Vehicle());

    public SafeAtomicFieldUsage() {
        Thread t1 = new Thread(() -> vehicle.get().setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private void createVehicle() {
        vehicle.get().setModel("Honda");
        vehicle.get().setYear(2020);
        vehicle.get().setColor("Red");
        vehicle.get().setPrice(20000);
    }
}
