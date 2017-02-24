package bugIdeas;

import java.util.Date;

public class Ideas_2010_08_12 {

    Date getDateFromSeconds(int i) {
        return new Date(i);
    }

    Date getDateFromMilliseconds(int i) {
        return new Date(i * 1000);
    }

}
