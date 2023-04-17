package FindHidingSubClassTest;

class GrantAccessStatic {
    public static void displayAccountStatus(String s, int i) {
        System.out.println("Account details for admin: XX");
    }
}

class GrantUserAccessStatic extends GrantAccessStatic {
    public static void displayAccountStatus(String s, int i) {
        System.out.println("Account details for user: XX");
    }
}
public class BadFindHidingSubClassTest {
    public static void choose(String username) {
        GrantAccessStatic admin = new GrantAccessStatic();
        GrantAccessStatic user = new GrantUserAccessStatic();
        if (username.equals("admin")) {
            admin.displayAccountStatus("", 0);
        } else {
            user.displayAccountStatus("", 0);
        }
    }

    public static void main(String[] args) {
        choose("user");
    }
}