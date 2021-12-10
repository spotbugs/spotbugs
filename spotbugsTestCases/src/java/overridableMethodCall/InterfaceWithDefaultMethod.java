package overridableMethodCall;

public interface InterfaceWithDefaultMethod {

  default int overridableDefaultMethod() {
    System.out.println("I am default and overridable.");
    return 1;
  }

}
