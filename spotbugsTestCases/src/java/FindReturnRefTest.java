import edu.umd.cs.findbugs.annotations.ExpectWarning;

import java.util.Date;
import java.util.HashMap;

public class FindReturnRefTest {
    private Date date;
    private Date[] dateArray;
    private HashMap<Integer, String> hm = new HashMap<Integer, String>();

    private static Date sDate = new Date();
    private static Date[] sDateArray = new Date[20];
    static {
        for (int i = 0; i < sDateArray.length; i++) {
            sDateArray[i] = new Date();
        }
    }
    private static HashMap<Integer, String> shm = new HashMap<Integer, String>();
    static {
        shm.put(1, "123-45-6789");
    }

    public FindReturnRefTest() {
        date = new Date();
        dateArray = new Date[20];
        hm.put(1, "123-45-6789");
        for (int i = 0; i < dateArray.length; i++) {
            dateArray[i] = new Date();
        }
    }

    // Returning a private mutable object reference attribute is dangerous.

    @ExpectWarning("EI")
    public Date getDate() {
        return date;
    }

    @ExpectWarning("MS")
    public static Date getStaticDate() {
        return sDate;
    }

    @ExpectWarning("EI") // Indirect way of returning reference
    public Date getDate2() {
        Date d = date;
        return d;
    }

    @ExpectWarning("MS") // Indirect way of returning reference
    public static Date getStaticDate2() {
        Date d = sDate;
        return d;
    }

    @ExpectWarning("EI")
    public Date[] getDateArray() {
        return dateArray;
    }

    @ExpectWarning("MS")
    public static Date[] getStaticDateArray() {
        return sDateArray;
    }

    @ExpectWarning("EI") // Cloning the array does not perform deep copy
    public Date[] getDateArray2() {
        return dateArray.clone();
    }

    @ExpectWarning("MS") // Cloning the array does not perform deep copy
    public static Date[] getStaticDateArray2() {
        return sDateArray.clone();
    }

    @ExpectWarning("EI")
    public HashMap<Integer, String> getValues() {
        return hm;
    }

    @ExpectWarning("MS")
    public static HashMap<Integer, String> getStaticValues() {
        return shm;
    }

    @ExpectWarning("EI2")
    public void setDate(Date d) {
        date = d;
    }

    @ExpectWarning("MS")
    public static void setStaticDate(Date d) {
        sDate = d;
    }

    // Storing a reference to a mutable object is dangerous.

    @ExpectWarning("EI2") // Indirect way of storing reference
    public void setDate2(Date d) {
        Date d2 = d;
        date = d2;
    }

    @ExpectWarning("MS") // Indirect way of storing reference
    public static void setStaticDate2(Date d) {
        Date d2 = d;
        sDate = d;
    }

    @ExpectWarning("EI2")
    public void setDateArray(Date[] da) {
        dateArray = da;
    }

    @ExpectWarning("MS")
    public static void setStaticDateArray(Date[] da) {
        sDateArray = da;
    }

    @ExpectWarning("EI2") // Cloning the array does not perform deep copy
    public void setDateArray2(Date[] da) {
        dateArray = da.clone();
    }

    @ExpectWarning("MS") // Cloning the array does not perform deep copy
    public static void setStaticDateArray2(Date[] da) {
        sDateArray = da.clone();
    }

    @ExpectWarning("EI2")
    public void setValues(HashMap<Integer, String> values) {
        hm = values;
    }

    @ExpectWarning("MS")
    public static void getStaticValues(HashMap<Integer, String>  values) {
        shm = values;
    }
}
