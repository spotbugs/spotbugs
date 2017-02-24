package bugIdeas;

public class Ideas_2010_05_04 {

    volatile int x;

    volatile long y;

    void bad() {
        x++;
        y++;
        x--;
        y--;
        x -= 2;
        y -= 2;
    }

}
