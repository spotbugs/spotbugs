package npe;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ReadLineTest {

    public static void main(String args[]) throws Exception {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String x = in.readLine();
        System.out.println(x.hashCode());

        x = in.readLine();
        System.out.println(x.hashCode());

        System.out.println(in.readLine().hashCode());

        System.out.println(in.readLine().hashCode());
    }

}
