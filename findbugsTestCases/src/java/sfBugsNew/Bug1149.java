package sfBugsNew;

public class Bug1149 {

    static class Foo {
        public void setEnabled(boolean b) {
        }
    }

    static class Bar {
        public void setEnabled(boolean b) {
        }

        class Nested extends Foo {
            public Nested() {
                this.setEnabled(true);
            }
            public Nested(boolean b) {
                setEnabled(b);
            }
            public Nested(int x) {
                super.setEnabled(x > 0);
            }
        }
    }

}
