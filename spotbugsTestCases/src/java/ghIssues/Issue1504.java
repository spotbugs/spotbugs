package ghIssues;

import java.util.ArrayList;
import java.util.Random;

public class Issue1504  {
    void test01(){
        int randNumber = new java.util.Random().nextInt(254);
    }

    void test02() {
        int randNumber = new Random().nextInt(254);
    }
}
