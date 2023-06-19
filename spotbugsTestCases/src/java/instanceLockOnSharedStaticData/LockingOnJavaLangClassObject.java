package instanceLockOnSharedStaticData;

public class LockingOnJavaLangClassObject {
    private static int counter = 0;

    public static int incrementCounter() {
        synchronized (LockingOnJavaLangClassObject.class) {
            return counter++;
        }
    }
}
