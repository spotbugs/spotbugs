
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import sun.util.BuddhistCalendar;


/**
 * Sample class for the StaticCalendarDetector
 * @author ds
 */
public class StaticCalender {

    /** static test field */
    public static final Calendar calStatic = Calendar.getInstance();

    /** static test field for subclass */
    private static Calendar calStatic2 = new BuddhistCalendar();

    /** instance test field */
    private Calendar calInstace = new GregorianCalendar();

    /** static test field */
    private static final SimpleDateFormat frmStatic = new SimpleDateFormat();

    /** static test field for different subclass */
    private static DateFormat frmStatic2 = new CustomDateFormat();

    /** instance test field */
    private DateFormat frmInstance = new CustomDateFormat();

    /** instance method to try and mask the static field */
    private Calendar getCal() {
        Calendar tMyCal = calStatic; // should fire
        return tMyCal;
    }

    /** instance method to try and mask the static field */
    private DateFormat getFrm() {
        DateFormat tMyFrm = frmStatic; // should fire
        return tMyFrm;
    }

    /** provoke hits */
    public void testDateFormats() {
        // test 1
        DateFormat tFrm1 = frmStatic; // should fire
        if (tFrm1.equals(frmStatic)) // hide from unused variable detector; should fire
            System.out.println("Frm1 equals frmStatic");

        // test 2
        DateFormat tFrm2 = getFrm();
        if (System.currentTimeMillis() < 1L) return; // some other code in between
        if (tFrm2.equals(frmStatic)) return; // should fire

        // test 3
        DateFormat tCal3 = frmInstance; 
        tCal3.setLenient(true); // should not fire
        
        // test 4
        DateFormat tCal4 = frmStatic2; // should fire
        
        int tInt = 1; // some other code in between
        boolean tBoolean = false; // some other code in between
        Object tObj = new Object(); // some other code in between
        if (tObj.hashCode() > 0) return; // some other code in between

        tCal4.setLenient(true); // should fire

        tCal4 = new SimpleDateFormat(); 
        tCal4.setLenient(true); // should not fire
    }

    /** provoke hits */
    public void testCalendars() {
        // test 1
        Calendar tCal1 = calStatic; // should fire
        if (tCal1.equals(calStatic)) // hide from unused variable detector; shoud fire 
            System.out.println("Cal1 equals calStatic");
        
        // test 2
        Calendar tCal2 = getCal();
        if (System.currentTimeMillis() < 1L) return; // some other code in between
        if (tCal2.equals(calStatic)) return; // should fire
        
        // test 3
        Calendar tCal3 = calInstace;
        tCal3.clear(); // should fire
        
        // test 4
        Calendar tCal4 = calStatic2; // should fire
        
        int tInt = 1; // some other code in between
        boolean tBoolean = false; // some other code in between
        Object tObj = new Object(); // some other code in between
        if (tObj.hashCode() > 0) return; // some other code in between
        
        tCal4.clear(); // should fire
        
        tCal4 = new GregorianCalendar(); 
        tCal4.clear(); // should not fire
    }
}


/**
 * Subclass of {@link DateFormat} for the detector to find.
 * @author ds
 */
class CustomDateFormat extends DateFormat {

    private static final long serialVersionUID = -5673759627954372347L;

    /*
     * (non-Javadoc)
     * 
     * @see java.text.DateFormat#format(java.util.Date, java.lang.StringBuffer,
     *      java.text.FieldPosition)
     */
    //@Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        if (fieldPosition == null) return null;
        return toAppendTo.append(date.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.text.DateFormat#parse(java.lang.String,
     *      java.text.ParsePosition)
     */
    //@Override
    public Date parse(String source, ParsePosition pos) {
        if (source == null || pos == null) return null;
        return new Date(1);
    }

}
