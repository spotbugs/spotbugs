package sfBugs;


public class Bug3463048 {
    private int onlyAssignedInConstructor;
    public Bug3463048() {
        onlyAssignedInConstructor = 10;
    }
    public void something() {
        System.out.println(onlyAssignedInConstructor);
    }
}
