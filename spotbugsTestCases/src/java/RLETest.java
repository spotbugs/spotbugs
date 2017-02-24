public class RLETest {
    public static class Box {
        Object value;

        public Object getValue() {
            synchronized (this) {
                return this.value;
            }
        }

        public void setValue(Object value) {
            synchronized (this) {
                this.value = value;
            }
        }

        public void setValue2(Object value) {
            synchronized (this) {
                this.value = value;
            }
        }

        public void setValue3(Object value) {
            synchronized (this) {
                this.value = value;
            }
        }
    }

    Box box = new Box();

    public static Object f(RLETest obj) {
        synchronized (obj.box) {
            return obj.box.value;
        }
    }

    public static void y(RLETest obj, Object value) {
        synchronized (obj.box) {
            obj.box.value = value;
        }
    }

    /*
     * public static void z(RLETest obj, Object value) { synchronized (obj.box)
     * { obj.box.value = value; } }
     */
}
