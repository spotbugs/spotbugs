package puzzlers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Puzzlers2007 {

    @ExpectWarning("GC")
    public static void shortSet() {
        Set<Short> s = new HashSet<Short>();
        for (short i = 0; i < 100; i++) {
            s.add(i);
            s.remove(i - 1);
        }
        System.out.println(s.size());
    }

    private static final String[] URL_NAMES = { "http://javapuzzlers.com", "http://apache2-snort.skybar.dreamhost.com",
            "http://www.google.com", "http://javapuzzlers.com", "http://findbugs.sourceforge.net", "http://www.cs.umd.edu" };

    @ExpectWarning("Dm")
    public static void urlSet() throws MalformedURLException {
        Set<URL> favorites = new HashSet<URL>();
        for (String urlName : URL_NAMES)
            favorites.add(new URL(urlName));
        System.out.println(favorites.size());
    }

    public static class Test extends junit.framework.TestCase {
        int number;

        public void test() throws InterruptedException {
            number = 0;
            Thread t = new Thread(new Runnable() {
                @Override
                @ExpectWarning("IJU")
                public void run() {
                    assertEquals(2, number);
                }
            });
            number = 1;
            t.start();
            number++;
            t.join();
        }
    }

    public static class Elvis {
        // Singleton pattern: there's only one Elvis
        @ExpectWarning("SI")
        public static final Elvis ELVIS = new Elvis();

        private Elvis() {
        }

        private static final Boolean LIVING = true;

        private final Boolean alive = LIVING;

        public final Boolean lives() {
            return alive;
        }

        public static void main(String[] args) {
            System.out.println(ELVIS.lives() ? "Hound Dog" : "Heartbreak Hotel");
        }
    }

    private static final int GAP_SIZE = 10 * 1024;

    @ExpectWarning("RR")
    public static void gap() throws IOException {
        File tmp = File.createTempFile("gap", ".txt");
        FileOutputStream out = new FileOutputStream(tmp);
        out.write(1);
        out.write(new byte[GAP_SIZE]);
        out.write(2);
        out.close();
        InputStream in = new BufferedInputStream(new FileInputStream(tmp));
        int first = in.read();
        in.skip(GAP_SIZE);
        int last = in.read();
        System.out.println(first + last);
    }

    private static final String[] words = { "I", "recommend", "polygene", "lubricants" };

    @ExpectWarning("RV")
    public static void histogram() {
        int[] histogram = new int[5];
        for (String word1 : words) {
            for (String word2 : words) {
                String pair = word1 + word2;
                int bucket = Math.abs(pair.hashCode()) % histogram.length;
                histogram[bucket]++;
            }
        }
        int pairCount = 0;
        for (int freq : histogram)
            pairCount += freq;
        System.out.println('C' + pairCount);
    }

    @ExpectWarning("Bx")
    public static void hamlet() {
        Random rnd = new Random();
        boolean toBe = rnd.nextBoolean();
        Number result = (toBe || !toBe) ? new Integer(3) : new Float(1);
        System.out.println(result);
    }

    public static int hamletFP(Integer x) {
        return (x != null) ? x : 0;
    }

    @ExpectWarning("ICAST")
    public static void round() {
        Random rnd = new Random();
        int i = rnd.nextInt();
        if (Math.round(i) != i)
            System.out.println("Ground Round");
    }
}
