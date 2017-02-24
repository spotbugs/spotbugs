package sfBugs;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug3502202 {
    static class Foo {
        @NonNull
        private String bar; // <=== Not initialized by constructor

        public int getBarLength() {
            return bar.length();
        }

        public void setBar(String bar) {
            this.bar = bar;
        }
    }

    // Furthermore, the following 'getBarLenght()' method will throw a
    // NullPointerException because the 'bar' field is not initialised. This is
    // not detected either.

    static class Baz {
        public int getBarLength() {
            return new Foo().getBarLength();
        }

        public static void main(String[] arg) {
            System.out.println("bar length =" + new Baz().getBarLength());
        }
    }
}
