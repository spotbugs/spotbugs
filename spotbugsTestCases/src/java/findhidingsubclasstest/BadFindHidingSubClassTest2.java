package findhidingsubclasstest;

class SuperClass {
    public static void displayAccountStatus(String s, int i) {
        System.out.println("Account details for admin: XX");
    }
}

class SubClass extends SuperClass {
    public static void displayAccountStatus(String s) {
        System.out.println("Account details for user: XX");
    }
}

//This class is the non-compliant test case for the bug.
//As the displayAccountStatus() is being hided by subclass and invocation is on instance variable.
//This test case is here to test the sub-signature check.
public class BadFindHidingSubClassTest2 {
    public static void choose(String username) {
        SuperClass admin = new SuperClass();
        SuperClass user = new SubClass();

        admin.displayAccountStatus("", 0);
        user.displayAccountStatus("", 1);
    }
}
