package publicIdentifiers;

public class ShadowedPublicIdentifier {
    public void shadowedVector() {
        Vector v = new Vector();
        if (v.isEmpty()) {
            System.out.println("Vector is empty");
        } else {
            System.out.println("Vector is not empty");
        }
    }

    public void shadowedBuffer() {
        Buffer b = new Buffer();
        if (b.hasRemaining()) {
            System.out.println("Buffer has remaining");
        } else {
            System.out.println("Buffer has no remaining");
        }
    }

    public void shadowedFile() {
        File f = new File("test.txt");
        if (f.isHidden()) {
            System.out.println("File is hidden");
        } else {
            System.out.println("File is not hidden");
        }
    }

    public void shadowedBitInteger() {
        BigInteger b = new BigInteger("-5121");
        if (b.signum() == 1) {
            System.out.println("BigInteger is positive");
        } else {
            System.out.println("BigInteger is not positive");
        }
    }

    private static class Vector {
        private final int size = 1;

        public boolean isEmpty() {
            return size == 1;
        }
    }

    private static class Buffer {
        public Buffer() {
        }

        public boolean hasRemaining() {
            return true;
        }
    }

    private static class File {
        private final String name;
        private final boolean hidden;

        public File(String name) {
            this.name = name;
            this.hidden = true;
        }

        public boolean isHidden() {
            return hidden;
        }
    }

    private static class BigInteger {
        private final String number;
        private int signum = 0;

        public BigInteger(String s) {
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
