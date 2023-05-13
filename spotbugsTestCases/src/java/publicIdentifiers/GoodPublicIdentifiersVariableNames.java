package publicIdentifiers;

public class GoodPublicIdentifiersVariableNames {
    private final Integer myInt = 8;

    public void myMethod() {
        Integer localInt = 466;
        System.out.println(myInt);

        String sysVar = "System";
    }

    public static void myStaticMethod() {
        System.out.println("Static method");
    }

    private void myMethodWithParams(Integer param1, String param2) {
        System.out.println(param1 + param2);
    }
}
