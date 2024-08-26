package ghIssues;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/637">GitHub issue</a>
 */
public final class Issue637NoErrors {

    private static final String AM_PM_HOUR_1_12_CORRECT = "h:mm a";
    private static final String AM_PM_HOUR_0_11_CORRECT = "K:mm a";
    private static final String MILITARY_HOUR_0_23_CORRECT = "HH:mm";
    private static final String MILITARY_HOUR_1_24_CORRECT = "kk:mm";
    private static final String WEEK_YEAR_CORRECT = "YYYY-MM-dd 'W'ww";

    private Issue637NoErrors(){
        //To prevent instantiation
    }

    /**
     * Expects no errors when proper flag combinations are used in one argument constructor.
     */
    public static void oneArgConstructorTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Expects no errors when proper flag combinations are used in two argument constructor with specified Locale
     */
    public static void twoArgLocaleConstructorTest(){
        Locale defaultLocale = new Locale("en", "US");

        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT, defaultLocale);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT, defaultLocale);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT, defaultLocale);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT, defaultLocale);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT, defaultLocale);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Expects no errors when proper flag combinations are used in two argument constructor with specified date
     * format symbols
     */
    public static void twoArgDateFormatSymbolConstructorTest(){
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());

        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT, dateFormatSymbols);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT, dateFormatSymbols);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT, dateFormatSymbols);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT, dateFormatSymbols);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT, dateFormatSymbols);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Expects no errors when proper flag combinations are used in "applyPattern" function
     */
    public static void applyPatternTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat();
        SimpleDateFormat hour12format2 = new SimpleDateFormat();
        SimpleDateFormat hour24format1 = new SimpleDateFormat();
        SimpleDateFormat hour24format2 = new SimpleDateFormat();
        SimpleDateFormat weekYear = new SimpleDateFormat();

        hour12format1.applyPattern(AM_PM_HOUR_1_12_CORRECT);
        hour12format2.applyPattern(AM_PM_HOUR_0_11_CORRECT);
        hour24format1.applyPattern(MILITARY_HOUR_0_23_CORRECT);
        hour24format2.applyPattern(MILITARY_HOUR_1_24_CORRECT);
        weekYear.applyPattern(WEEK_YEAR_CORRECT);
    }

    /**
     * Expects no errors when proper format flag combinations are used in "applyLocalizedPattern" function
     */
    public static void applyLocalizedPatternTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT);

        String localizedHour12format1 = hour12format1.toLocalizedPattern();
        String localizedHour12format2 = hour12format2.toLocalizedPattern();
        String localizedHour24format1 = hour24format1.toLocalizedPattern();
        String localizedHour24format2 = hour24format2.toLocalizedPattern();
        String localizedWeekYear = weekYear.toLocalizedPattern();

        hour12format1.applyLocalizedPattern(localizedHour12format1);
        hour12format2.applyLocalizedPattern(localizedHour12format2);
        hour24format1.applyLocalizedPattern(localizedHour24format1);
        hour24format2.applyLocalizedPattern(localizedHour24format2);
        weekYear.applyLocalizedPattern(localizedWeekYear);
    }
}
