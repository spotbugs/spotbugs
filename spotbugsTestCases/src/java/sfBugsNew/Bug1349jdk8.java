package sfBugsNew;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1349jdk8 {
    private final ConcurrentMap<String, Calendar> calendars = new ConcurrentHashMap<>();
    private final Map<String, Calendar> newCalendars = new ConcurrentHashMap<>();
    private final Map<String, Calendar> hashMapCalendars = new HashMap<>();

    @ExpectWarning("RV_RETURN_VALUE_OF_PUTIFABSENT_IGNORED")
    public Calendar update(String key, Calendar newValue) {
        calendars.putIfAbsent(key, newValue);
        newValue.add(Calendar.DAY_OF_MONTH, 2);
        return newValue;
    }

    @ExpectWarning("RV_RETURN_VALUE_OF_PUTIFABSENT_IGNORED")
    public Calendar newUpdate(String key, Calendar newValue) {
        newCalendars.putIfAbsent(key, newValue);
        newValue.add(Calendar.DAY_OF_MONTH, 2);
        return newValue;
    }

    @NoWarning("RV_RETURN_VALUE_OF_PUTIFABSENT_IGNORED")
    public Calendar hashMapUpdate(String key, Calendar newValue) {
    	hashMapCalendars.putIfAbsent(key, newValue);
        newValue.add(Calendar.DAY_OF_MONTH, 2);
        return newValue;
    }
}
