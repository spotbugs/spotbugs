package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1253 {

    public String name;

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @NoWarning("NP")
    public void method2(int x) {
        Bug1253 p = null;
        if (x > 0) {
            p = new Bug1253();
            p.setName("name1");
        }
        if (x < 0) {
            p.setName("name2");
        }
    }
}
