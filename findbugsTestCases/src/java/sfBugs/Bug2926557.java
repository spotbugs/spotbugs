package sfBugs;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class Bug2926557 {

    private static final Calendar cal = Calendar.getInstance();

    private static final ReentrantLock lock = new ReentrantLock();

    public static Date changeMonth(Date month, int value) {
        lock.lock();
        try {
            cal.setTime(month);
            cal.add(Calendar.MONTH, value);
            return cal.getTime();
        } finally {
            lock.unlock();
        }

    }
}
