package publicIdentifiers;

public class GoodPublicIdentifiersMethodNames {
    public static void myStaticMethod() {
        System.out.println("Static method");
    }

    private void myMethodWithParams(Integer param1, String param2) {
        System.out.println(param1 + param2);
    }

    public int getIntValueFromDouble(double myValue) {
        return (int) myValue;
    }
}
