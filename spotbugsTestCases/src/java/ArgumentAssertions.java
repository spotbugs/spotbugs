public class ArgumentAssertions {
    // Should fail
    public static int getAbsAdd(int x, int y) {
        assert x != Integer.MIN_VALUE;
        assert y != Integer.MIN_VALUE;
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        assert (absX <= Integer.MAX_VALUE - absY);
        return absX + absY;
    }

    // Should fail
    public static int getAbsAdd2(int x, int y) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        assert y != Integer.MIN_VALUE;
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        assert (absX <= Integer.MAX_VALUE - absY);
        return absX + absY;
    }

    private static int plus1(int x) {
        return x + 1;
    }

    // Should fail
    public static int getAbsAdd3(int x, int y) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        assert plus1(y) != Integer.MIN_VALUE;
        assert Math.abs(plus1(y)) != Integer.MIN_VALUE;
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        assert (absX <= Integer.MAX_VALUE - absY);
        return absX + absY;
    }

    // Should fail
    public static int getAbsAdd4(int x, int y) {
        assert Math.abs(plus1(y)) != Integer.MIN_VALUE;
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        return absX + absY;
    }

    // Should fail
    public static Object print(Object x) {
        assert x != null;
        Double f = 3.0;
        System.out.println(f.toString());
        return x;
    }

    private static String helper(String s) {
        return s;
    }

    // Should fail
    public static void indirect(String s) {
        assert helper(s) == null : "This is not a false pos" + s;
    }

    // Should fail
    public static boolean compBool1(boolean b) {
        assert b == true;
        return b;
    }

    // Should fail
    public static boolean compBool2(boolean b) {
        assert b;
        return b;
    }

    // Should fail
    public static int getAbs(byte x) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        int absX = Math.abs(x);
        return absX;
    }

    // Should fail
    public static int getAbs(short x) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        int absX = Math.abs(x);
        return absX;
    }

    // Should fail
    public static int compShor(short s) {
        assert s == 0;
        return s;
    }

    // Should fail
    public static long getAbs(long x) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        long absX = Math.abs(x);
        return absX;
    }

    // Should fail
    public static long compLong(long l) {
        assert l == 0;
        return l;
    }

    // Should fail
    public static float getAbs(float x) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        float absX = Math.abs(x);
        return absX;
    }

    // Should fail
    public static float compFloat(float f) {
        assert f < 0;
        return f;
    }

    // Should fail
    public static double getAbs(double x) {
        assert Math.abs(x) != Integer.MIN_VALUE;
        double absX = Math.abs(x);
        return absX;
    }

    // Should fail
    public static double compDouble(double d) {
        assert d > 0;
        return d;
    }

    // Should fail
    public static boolean indirect(boolean b) {
        boolean l = b;
        assert l;
        return l;
    }

    // Should fail, but passes because indirect assertions of arguments are not supported yet (except copies)
    public static boolean indirect2(boolean b) {
        boolean l = !b;
        assert l;
        return l;
    }

    // Should pass
    public static String literal(String s) {
        assert false;
        return s;
    }

    // Should pass
    public static boolean literalAndMessage(boolean b) {
        assert false : b;
        return b;
    }

    // Should pass
    public static String literalAndMessageStr(String s) {
        assert false : s;
        return s;
    }

    // Should pass
    public static void conditionallyInMessage(int x, String s) {
        if (x < 0) {
            assert false : "This is a false pos" + s;
        }
    }

    // helper method
    public void helper() {
        privateMethod(1);
        privateFinalMethod(1);
        privateStaticMethod(0);
    }

    // Should pass - private method
    private void privateMethod(int x) {
        assert x != 0;
    }

    // Should pass - private method
    private final void privateFinalMethod(int x) {
        assert x != 0;
    }

    // Should pass - private method
    private static void privateStaticMethod(int x) {
        assert x == 0;
    }

    // Should fail
    public static void assertingArgInFor(String prefix) {
        java.util.List<String> strings = new java.util.ArrayList<>();
        for (String s : strings) {
            assert s.startsWith(prefix);
        }
    }

    // Should fail
    public static void assertingArgInStream(String prefix) {
        java.util.List<String> strings = new java.util.ArrayList<>();
        strings.forEach(s -> {
            assert s.startsWith(prefix);
        });
    }
}
