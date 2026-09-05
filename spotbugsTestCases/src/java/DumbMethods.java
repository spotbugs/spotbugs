import java.util.Collection;

class DumbMethods {

    public static String getStringOfString(String s) {
        return s.toString();
    }

    public static boolean isCollection(Object o) {
        return ((o != null) && (o instanceof Collection));
    }
}
