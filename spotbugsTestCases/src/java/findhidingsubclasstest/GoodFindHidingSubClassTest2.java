package findhidingsubclasstest;

class GrantAccessStatic2 {
    public static void displayAccountStatus(String s, int i) {
        System.out.println("Account details for admin: XX");
    }
}
//This class is the compliant test case for the bug.
//As the displayAccountStatus() is called with fully qualified class name.
class GrantUserAccessStatic2 extends GrantAccessStatic2 {
    public static void displayAccountStatus(String s, int i) {
        System.out.println("Account details for user: XX");
    }
}
public class GoodFindHidingSubClassTest2 {
    public static void choose(String username) {
        if (username.equals("admin")) {
            GrantAccessStatic2.displayAccountStatus("", 0);
        } else {
            GrantUserAccessStatic2.displayAccountStatus("", 0);
        }
    }
}
