package bugIdeas;

public class Ideas_2008_09_17 {

    public static int getInt1(String s) {
        long result = Long.parseLong(s);
        return (int) (result & 0xEFFFFFFF);
    }

    public static int getInt2(String s) {
        long result = Long.parseLong(s);
        return (int) (0xEFFFFFFF & result);
    }

    public static int getInt3(String s) {
        long result = Long.parseLong(s);
        return (int) (result) & 0xEFFFFFFF;
    }

    public static int getInt4(String s) {
        long result = Long.parseLong(s);
        return 0xEFFFFFFF & (int) result;
    }

    public static int getInt5(String s) {
        int result = Integer.parseInt(s);
        return (result & 0xEFFFFFFF);
    }

    public static int getInt6(String s) {
        int result = Integer.parseInt(s);
        return (0xEFFFFFFF & result);
    }

    public static long getLong1(String s) {
        long result = Long.parseLong(s);
        return (result & 0xEFFFFFFFFFFFFFFFL);
    }

    public static long getLong2(String s) {
        long result = Long.parseLong(s);
        return (0xEFFFFFFFFFFFFFFFL & result);
    }

}
