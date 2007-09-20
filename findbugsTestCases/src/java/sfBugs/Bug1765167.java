package sfBugs;

/**
 * Submitted By: Ian Macfarlane
 * Summary:
 * 
 * When assigning values to a variable in the middle of an if statement,
 * FindBugs sometimes seems to think that the statement does nothing.
 * 
 * In the attached test case, the code looking for the index of characters
 * like 'st' or 'th' in Strings like '21st' or '2nd'. However, FindBugs says
 * that the code does nothing.
 * 
 * Oddly enough, if you take out some of the lines in the long
 * if-elseif-elseif-etc block (from the bottom up to around the 3rd/3th
 * entries) the bug warning vanishes.
 */
public class Bug1765167 {
    
    //// grep -A 1 UCF_USELESS_CONTROL_FLOW | grep Bug1765167
    public static void main(String[] args) {
        Bug1765167 b = new Bug1765167();
        b.method1();
        b.method2();
    }
    
    public void method1() {
        String newSource = "Wednesday 25th";

        //try removing st/nd/rd/th

        String ignoreCaseNewSource = newSource.toLowerCase();

        int thTypeCharsIndex = -1;

        if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("1st")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("1th")) != -1);//11th
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("2nd")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("2th")) != -1);//12th
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("3rd")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("3th")) != -1);//13th
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("4th")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("5th")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("6th")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("7th")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("8th")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("9th")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("0th")) != -1);

        System.out.println("Trailing date characters index = "+thTypeCharsIndex);
        System.out.println("ending");
    }
    
    public void method2() {
        String newSource = "Wednesday 25th";

        //try removing st/nd/rd/th

        String ignoreCaseNewSource = newSource.toLowerCase();

        int thTypeCharsIndex = -1;

        if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("1st")) != -1);
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("1th")) != -1);//11th
        else if ((thTypeCharsIndex = ignoreCaseNewSource.indexOf("2nd")) != -1);

        System.out.println("Trailing date characters index = "+thTypeCharsIndex);
        System.out.println("ending");
    }
}
