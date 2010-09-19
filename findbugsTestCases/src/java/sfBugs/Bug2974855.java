package sfBugs;

import java.net.URL;

public class Bug2974855 {

    public URL test() {
        URL u = this.getClass().getResource("");
        return u;
    }

    public static void main(String args[]) {
        System.out.println(new Bug2974855().test());
    }

}
