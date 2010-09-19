package sfBugs;

public class Bug2137182 {
    static class Base {
        public String getName() {
            return "base";
        }
    }

    static class Outer {
        Inner inner = new Inner();

        public String getName() {
            return "Outer";
        }

        public String foo() {
            return "foo";
        }

        class Inner extends Base {
            public void falsePositive() {
                System.out.println(this.getName());
            }
        }
    }
}
