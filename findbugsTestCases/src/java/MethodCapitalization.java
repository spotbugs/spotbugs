import edu.umd.cs.findbugs.annotations.ExpectWarning;

class MethodCapitalization {
    public void MethodCapitalization() {
    }

    public int getX() {
        return 42;
    }

    @ExpectWarning("Nm")
    public int hashcode() {
        return 42;
    }

    @ExpectWarning("Nm")
    public String tostring() {
        return "MethodCapitalization";
    }

    static class Foo extends MethodCapitalization {
        @ExpectWarning("Nm")
        public int getx() {
            return 42;
        }

    }

    static class Bar {
        public int getx() {
            return 42;
        }

        @ExpectWarning("Nm")
        public String ToString() {
            return "Bar";
        }
    }
}
