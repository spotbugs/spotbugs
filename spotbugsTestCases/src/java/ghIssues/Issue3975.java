package ghIssues;

public class Issue3975 {

    int intValue = 3;
    boolean intFlag = 1 > (intValue = 2); // reported before the fix

    long longValue = 3L;
    boolean longFlag = 1 > (longValue = 2L); // false negative before the fix

    double doubleValue = 3.0;
    boolean doubleFlag = 1 > (doubleValue = 2.0); // false negative before the fix

    long readBetween = 3L;
    boolean readFlag = readBetween > (readBetween = 2L); // the first value is read, no double assignment
}
