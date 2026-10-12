package sfBugs;

import org.jspecify.annotations.NonNull;

public class Bug3502202 {
    static class Foo {

        private @NonNull String bar; // <=== Not initialized by constructor

        public int getBarLength() {
            return bar.length();
        }

        public void setBar(String bar) {
            this.bar = bar;
        }
    }

    // Furthermore, the following 'getBarLenght()' method will throw a
    // NullPointerException because the 'bar' field is not initialized. This is
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
