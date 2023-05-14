package findhidingsubclasstest;

class GrantAccess {
    public void displayAccountStatus() {
        System.out.print("Account details for admin: XX");
    }
}

class GrantUserAccess extends GrantAccess {
    @Override
    public void displayAccountStatus() {
        System.out.print("Account details for user: XX");
    }
}

//This class is the compliant test case for the bug.
//As the displayAccountStatus() is declared as non-static.
public class GoodFindHidingSubClassTest {
    public static void choose(String username) {
        GrantAccess admin = new GrantAccess();
        GrantAccess user = new GrantUserAccess();

        if (username.equals("admin")) {
            admin.displayAccountStatus();
        } else {
            user.displayAccountStatus();
        }
    }

    public static void main(String[] args) {
        choose("user");
    }
}
