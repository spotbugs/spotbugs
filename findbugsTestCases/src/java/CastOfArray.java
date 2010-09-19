public class CastOfArray {
    public byte[] falsePositiveByteArray() {
        Object a = new byte[10];
        return (byte[]) a;
    }

    public Object[] falsePositiveObjectArray() {
        Object a = new Object[10];
        return (Object[]) a;
    }

    public Object[][] falsePositiveObjectArrayArray() {
        Object a = new Object[10][10];
        return (Object[][]) a;
    }

    public byte[][] falsePositiveByteArrayArray() {
        Object a = new byte[10][10];
        return (byte[][]) a;
    }

}
