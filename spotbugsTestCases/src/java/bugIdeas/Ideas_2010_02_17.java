package bugIdeas;

import java.io.Serializable;

//TODO understand why is this important to use internal classes here
//import sun.util.calendar.ZoneInfo;

public class Ideas_2010_02_17 implements Serializable {

//    ZoneInfo zone;
//
//    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//        final ObjectInputStream input = stream;
//        input.defaultReadObject();
//
//        // If there's a ZoneInfo object, use it for zone.
//        try {
//            ZoneInfo zi = AccessController.doPrivileged(new PrivilegedExceptionAction<ZoneInfo>() {
//                @Override
//                public ZoneInfo run() throws Exception {
//                    return (ZoneInfo) input.readObject();
//                }
//            });
//            if (zi != null) {
//                zone = zi;
//            }
//        } catch (Exception e) {
//        }
//
//    }
}
