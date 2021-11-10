package ghIssues;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/637">GitHub issue</a>
 */
public class Issue637Errors {

    private static final String AM_PM_HOUR_1_12_INCORRECT = "hh:mm";
    private static final String AM_PM_HOUR_0_11_INCORRECT = "KK:mm";
    private static final String MILITARY_HOUR_0_23_INCORRECT = "HH:mm aa";
    private static final String MILITARY_HOUR_1_24_INCORRECT = "kk:mm aa";
    private static final String WEEK_YEAR_INCORRECT = "YYYY-MM-dd";

    /**
     * Raises error when AM/PM hour "h" flag is used without "a" flag for AM/PM marker in SimpleDateFormat constructor
     */
    public static void constructorTest_AM_PM_HOUR_1_12(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AM_PM_HOUR_1_12_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when AM/PM hour "K" flag is used without "a" flag for AM/PM marker in SimpleDateFormat constructor
     */
    public static void constructorTest_AM_PM_HOUR_0_11(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AM_PM_HOUR_0_11_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when military time hour "H" flag is used with "a" flag for AM/PM marker in SimpleDateFormat
     * constructor
     */
    public static void constructorTest_MILITARY_HOUR_0_23(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(MILITARY_HOUR_0_23_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when military time hour "k" flag is used with "a" flag for AM/PM marker in SimpleDateFormat
     * constructor
     */
    public static void constructorTest_MILITARY_HOUR_1_24(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(MILITARY_HOUR_1_24_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when week-year flag "Y" is used with "M" and "d" flags without week flag "w" in SimpleDateFormat
     * constructor
     */
    public static void constructorTest_WEEK_YEAR(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when AM/PM hour "h" flag is used without "a" flag for AM/PM marker in SimpleDateFormat
     * "applyPattern" method
     */
    public static void applyPatternTest_AM_PM_HOUR_1_12(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(AM_PM_HOUR_1_12_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when AM/PM hour "K" flag is used without "a" flag for AM/PM marker in SimpleDateFormat
     * "applyPattern" method
     */
    public static void applyPatternTest_AM_PM_HOUR_0_11() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(AM_PM_HOUR_0_11_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     *  Raises error when military time hour "H" flag is used with "a" flag for AM/PM marker in SimpleDateFormat
     * "applyPattern" method
     */
    public static void applyPatternTest_MILITARY_HOUR_0_23(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(MILITARY_HOUR_0_23_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     * Raises error when military time hour "k" flag is used with "a" flag for AM/PM marker in SimpleDateFormat
     * "applyPattern" method
     */
    public static void applyPatternTest_MILITARY_HOUR_1_24() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(MILITARY_HOUR_1_24_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    /**
     *    Raises error when week-year flag "Y" is used with "M" and "d" flags without week flag "w" in SimpleDateFormat
     *    "applyPattern" method
     */
    public static void applyPatternTest_WEEK_YEAR(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(WEEK_YEAR_INCORRECT);
        System.out.println(simpleDateFormat.format(new Date()));
    }
}
