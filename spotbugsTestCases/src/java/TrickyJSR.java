public class TrickyJSR {

    public static void main(String[] argv) throws Throwable {

        final boolean P = Boolean.getBoolean("P");
        final boolean PP = Boolean.getBoolean("PP");
        final boolean QQ = Boolean.getBoolean("QQ");
        final boolean ZZ = Boolean.getBoolean("ZZ");

        int count = 0;

        loop: for (int i = 0; i < 10; ++i) {
            try {
                try {
                    count++;

                    if (PP) {
                        System.out.println("MMM");
                        throw new Throwable("X");
                    }

                    if (QQ)
                        break loop;

                    if (ZZ)
                        throw new Throwable("ZX");

                } finally {
                    count++;
                    count += Math.max(count, ((count ^ 0xff) & 0x3) != 0 ? 10 : 20);
                    if (P)
                        throw new Exception("hah!");
                }
            } catch (Exception e) {
                System.out.println("Yeah");
            }
        }

        System.out.println("count=" + count);

    }
}

// vim:ts=4
