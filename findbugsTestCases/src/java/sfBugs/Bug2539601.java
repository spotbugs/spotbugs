package sfBugs;

public class Bug2539601 {
    int i;

    class Inner1 {
        {
            System.out.println(i);
        }

        class Inner2 {

        }
    }
}
