package sfBugs;

public class Bug2931203 {

    public static void main(String args[]) {
        if ("Test".startsWith("T")) {
            System.out.println("Yes it starts with a T");
        } else {
            System.out.println("No it doesn't start with a T");
        }

    }

}
