package sfBugs;

public class Bug1678229 {

	private static String staticDevice;
	private static StringBuffer staticDeviceFactory;

	public void registerDeviceFactory(StringBuffer factory) {
		staticDeviceFactory = factory;
	}

	public int open() {
		if (staticDevice == null) {
			staticDevice = staticDeviceFactory.toString();
        }
		return staticDevice.hashCode();
	}

	private  String device;
	private  StringBuffer deviceFactory;

	public  void registerDeviceFactory2(StringBuffer factory) {
		deviceFactory = factory;
	}

	public int open2() {
		if (device == null) {
			device = deviceFactory.toString();
        }
		return device.hashCode();
	}



}
