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
public class BadFindHidingSubClassTest2 {
    public static void choose(String username) {
        SuperClass admin = new SuperClass();
        SuperClass user = new SubClass();

        admin.displayAccountStatus("", 0);
        user.displayAccountStatus("", 1);
    }
}
