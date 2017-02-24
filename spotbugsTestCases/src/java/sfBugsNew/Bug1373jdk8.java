package sfBugsNew;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1373jdk8 {
    @NoWarning("UC")
    public void test() {
        List<String> a = new ArrayList<>();
        a.add("A");
        a.forEach(System.out::println);
    }
}
