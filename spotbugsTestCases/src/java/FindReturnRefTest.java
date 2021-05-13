import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

import java.util.Date;
import java.util.Hashtable;

public class FindReturnRefTest {
    private Date date;
    private Date[] dateArray;
    private Hashtable<Integer, String> ht = new Hashtable<Integer, String>();

    private static Date sDate = new Date();
    private static Date[] sDateArray = new Date[20];
    static {
        for (int i = 0; i < sDateArray.length; i++) {
            sDateArray[i] = new Date();
        }
    }
    private static Hashtable<Integer, String> sht = new Hashtable<Integer, String>();
    static {
        sht.put(1, "123-45-6789");
    }

    public FindReturnRefTest() {
        date = new Date();
        dateArray = new Date[20];
        ht.put(1, "123-45-6789");
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

    @DesireWarning("EI") // Indirect way of returning reference
    public Date getDate2() {
        Date d = date;
        return d;
    }

    @DesireWarning("MS") // Indirect way of returning reference
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

    @DesireWarning("EI") // Cloning the array does not perform deep copy
    public Date[] getDateArray2() {
        return dateArray.clone();
    }

    @DesireWarning("MS") // Cloning the array does not perform deep copy
    public static Date[] getStaticDateArray2() {
        return sDateArray.clone();
    }

    @ExpectWarning("EI")
    public Hashtable<Integer, String> getValues() {
        return ht;
    }

    @ExpectWarning("MS")
    public static Hashtable<Integer, String> getStaticValues() {
        return sht;
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

    @DesireWarning("EI2") // Cloning the array does not perform deep copy
    public void setDateArray2(Date[] da) {
        dateArray = da.clone();
    }

    @DesireWarning("MS") // Cloning the array does not perform deep copy
    public static void setStaticDateArray2(Date[] da) {
        sDateArray = da.clone();
    }

    @ExpectWarning("EI2")
    public void setValues(Hashtable<Integer, String> values) {
        ht = values;
    }

    @ExpectWarning("MS")
    public static void getStaticValues(Hashtable<Integer, String>  values) {
        sht = values;
    }
}
