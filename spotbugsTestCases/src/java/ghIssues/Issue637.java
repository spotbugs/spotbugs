package ghIssues;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/">GitHub issue</a>
 */
public class Issue637 {

    private static final String AM_PM_HOUR_1_12_INCORRECT = "hh:mm";
    private static final String AM_PM_HOUR_1_12_CORRECT = "hh:mm aa";

    private static final String AM_PM_HOUR_0_11_INCORRECT = "KK:mm";
    private static final String AM_PM_HOUR_0_11_CORRECT = "KK:mm aa";

    private static final String WEEK_YEAR_INCORRECT = "YYYY-MM-dd";
    private static final String WEEK_YEAR_CORRECT = "YYYY-MM-dd 'W'ww";

    public static String constructorTestFormatOneError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AM_PM_HOUR_1_12_INCORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String constructorTestFormatOneNoError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String constructorTestFormatTwoError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AM_PM_HOUR_0_11_INCORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String constructorTestFormatTwoNoError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String constructorTestFormatThreeError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_INCORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String constructorTestFormatThreeNoError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_CORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String applyPatternTestFormatOneError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(AM_PM_HOUR_1_12_INCORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String applyPatternTestFormatTwoError() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(AM_PM_HOUR_0_11_INCORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    public static String applyPatternTestFormatThreeError(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern(WEEK_YEAR_INCORRECT);
        String date = simpleDateFormat.format(new Date());
        return date;
    }

}
