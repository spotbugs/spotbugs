package findhidingsubclasstest;

class GrantAccessStatic {
    public static void displayAccountStatus(String s) {
        System.out.println("Account details for admin: XX " + s);
    }
}

class GrantUserAccessStatic extends GrantAccessStatic {
    public static void displayAccountStatus(String s) {
        System.out.println("Account details for user: XX " + s);
    }
}

//This class is the non-compliant test case for the bug.
//As the displayAccountStatus() is being hided by subclass.
public class BadFindHidingSubClassTest {
    public static void choose(String username) {
        GrantAccessStatic admin = new GrantAccessStatic();
        GrantAccessStatic user = new GrantUserAccessStatic();
        if (username.equals("admin")) {
            admin.displayAccountStatus("");
        } else {
            user.displayAccountStatus("");
        }
    }

    public static void main(String[] args) {
        choose("user");
    }
}