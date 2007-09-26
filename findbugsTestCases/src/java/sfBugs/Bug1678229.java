package sfBugs;

/**
 * Don't report UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR
 * http://sourceforge.net/tracker/index.php?func=detail&aid=1678229&group_id=96405&atid=614693
 * @author pugh
 *
 */
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
