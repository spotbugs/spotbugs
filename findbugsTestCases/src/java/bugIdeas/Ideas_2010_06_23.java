package bugIdeas;

import java.sql.Date;

import org.joda.time.DateTimeConstants;

public class Ideas_2010_06_23 {

    public long daysAgo(int days) {
        return System.currentTimeMillis() - days * DateTimeConstants.MILLIS_PER_DAY;
    }

    public long daysFromNow(int days) {
        return System.currentTimeMillis() + days * DateTimeConstants.MILLIS_PER_DAY;
    }

    public long daysFromNow2(int days) {
        return days * DateTimeConstants.MILLIS_PER_DAY + System.currentTimeMillis();
    }

    public long daysFrom(Date d, int days) {
        return days * DateTimeConstants.MILLIS_PER_DAY + d.getTime();
    }

    public long daysBefore(Date d, int days) {
        return d.getTime() - days * DateTimeConstants.MILLIS_PER_DAY;
    }

    public static void main(String args[]) {
        System.out.println(DateTimeConstants.MILLIS_PER_DAY);
        System.out.println(Integer.MAX_VALUE / DateTimeConstants.MILLIS_PER_DAY);
    }
}
