package commonResources;

import java.util.Collections;
import java.util.List;

public class SafeSynchronizedCollectionUsage {

    private final List<Vehicle> vehicle = Collections.synchronizedList(Collections.singletonList(new Vehicle()));

    public SafeSynchronizedCollectionUsage() {
        Thread t1 = new Thread(() -> vehicle.get(0).setModel("Toyota"));
        t1.start();

        Thread t2 = new Thread(this::createVehicle);
        t2.start();
    }

    private void createVehicle() {
        vehicle.get(0).setModel("Honda");
        vehicle.get(0).setYear(2020);
        vehicle.get(0).setColor("Red");
        vehicle.get(0).setPrice(20000);
    }
}
