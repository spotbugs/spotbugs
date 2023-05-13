package publicIdentifiers.inner;

import java.util.Vector;

public class GoodPublicIdentifiersInnerClassNames {
    public void shadowedVector() {
        Vector v = new Vector();
        if (v.isEmpty()) {
            System.out.println("Vector is empty");
        } else {
            System.out.println("Vector is not empty");
        }
    }

    public void shadowedBuffer() {
        MyBuffer b = new MyBuffer();
        if (b.hasRemaining()) {
            System.out.println("Buffer has remaining");
        } else {
            System.out.println("Buffer has no remaining");
        }
    }

    public void shadowedFile() {
        MyFile f = new MyFile("test.txt");
        if (f.isHidden()) {
            System.out.println("File is hidden");
        } else {
            System.out.println("File is not hidden");
        }
    }

    public void shadowedBitInteger() {
        MyBigInteger b = new MyBigInteger("-5121");
        if (b.signum() == 1) {
            System.out.println("BigInteger is positive");
        } else {
            System.out.println("BigInteger is not positive");
        }
    }

    static class MyFile {
        private final String name;
        private final boolean hidden;

        public MyFile(String name) {
            this.name = name;
            this.hidden = true;
        }

        public boolean isHidden() {
            return hidden;
        }
    }

    private static class MyBuffer {
        public boolean hasRemaining() {
            return true;
        }
    }

    protected static class MyBigInteger {
        private final String number;
        private int signum = 0;

        public MyBigInteger(String s) {
            this.number = s;
            if (s.startsWith("-")) {
                this.signum = 1; // - symbol is accidentally left out
            } else {
                this.signum = 1;
            }
        }

        public int signum() {
            return signum;
        }
    }
}
