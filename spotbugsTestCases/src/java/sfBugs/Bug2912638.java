package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug2912638 {

    private String name;

    public void setName(String _name) {
        name = _name;
    }

    public String getName() {
        return name;
    }

    @ExpectWarning("ES_COMPARING_STRINGS_WITH_EQ")
    protected static void findBugsTest(Bug2912638 person) {
        String value = person.getName();

        if ("FOO" == value)
            System.out.println("a");
        else
            System.out.println("a3");
        if (value == "FOO")
            System.out.println("a");
        else
            System.out.println("a3");
    }

    @ExpectWarning("ES_COMPARING_STRINGS_WITH_EQ")
    protected static void findBugsTest2(Bug2912638 person) {
        String value = person.getName();

        if (value == "FOO")
            System.out.println("a");
        else
            System.out.println("a3");
    }

}
