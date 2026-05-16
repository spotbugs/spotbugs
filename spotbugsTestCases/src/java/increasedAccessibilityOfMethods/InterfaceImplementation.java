package increasedAccessibilityOfMethods;

public class InterfaceImplementation implements Interface {
    @Override
    public String interfaceMethodPublicByDefault() {
        return "InterfaceImplementation.interfaceMethodPublicByDefault";
    }

    @Override
    public void publicInterfaceMethod() {
        System.out.println("InterfaceImplementation.publicInterfaceMethod");
    }
}
