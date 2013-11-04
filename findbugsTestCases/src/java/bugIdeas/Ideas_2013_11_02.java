package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2013_11_02 {

    int x;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void incrementGood() {
        x++;
    }

    @ExpectWarning("SA")
    public void incrementBad1() {
        x = x++;
    }

    @ExpectWarning("SA")
    public void incrementBad2() {
        x = ++x;
    }

    @ExpectWarning("SA")
    public void bad() {
        int tmp = x;
        x = tmp;
    }
}
