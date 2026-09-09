public class TestASM {
    public static final int GOOD_FIELD_NAME_PUBLIC_STATIC_FINAL = 0;
    public static final int badFieldNamePublicStaticFinal = 1;

    public static int goodFieldNamePublicStatic = 2;
    public static int BadFieldNamePublicStatic = 3;

    public int goodFieldNamePublic = 4;
    public int BadFieldNamePublic = 5;

    protected static int goodFieldNameProtectedStatic = 6;
    protected static int BadFieldNameProtectedStatic = 7;

    protected int goodFieldNameProtected = 8;
    protected int BadFieldNameProtected = 9;

    private int goodFieldNamePrivate = 10;
    private int BadFieldNamePrivate = 11;

    public void BadMethodName() {
        int integer = 2;
        double ceil = Math.ceil((double) integer);
    }

    public void goodMethodName() {
        double d = 4.0;
        double ceil = Math.ceil(d);
    }
}
