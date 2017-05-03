package bugIdeas;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Ideas_2009_10_10 {

    void test(SimpleDateFormat sdf, String date) throws Exception {
        DateFormat df = sdf;
        if (date == null)
            System.out.println("oops");
        date = null;
        Date d1 = df.parse(date);
        ParsePosition p = new ParsePosition(0);
        Date d2 = df.parse(date, p);
        Date d3 = sdf.parse(date);
        Date d4 = sdf.parse(date, p);
    }

}
