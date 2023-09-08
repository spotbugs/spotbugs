import edu.umd.cs.findbugs.annotations.ExpectWarning;

import java.nio.CharBuffer;
import java.util.Date;
import java.util.HashMap;

public class FindReturnRefTest2 {
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

    private CharBuffer charBuf;
    private char[] charArray;

    private static CharBuffer sCharBuf;
    private static char[] sCharArray;

    public FindReturnRefTest2() {
        date = new Date();
        dateArray = new Date[20];
        hm.put(1, "123-45-6789");
        for (int i = 0; i < dateArray.length; i++) {
            dateArray[i] = new Date();
        }
    }

    public static class Nested {
        // Returning a private mutable object reference attribute is dangerous.

        public static Date getStaticDate() {
            return sDate;
        }

        // Indirect way of returning reference
        public static Date getStaticDate2() {
            Date d = sDate;
            return d;
        }

        public static Date[] getStaticDateArray() {
            return sDateArray;
        }

        // Cloning the array does not perform deep copy
        public static Date[] getStaticDateArray2() {
            return sDateArray.clone();
        }

        public static HashMap<Integer, String> getStaticValues() {
            return shm;
        }

        public static void setStaticDate(Date d) {
            sDate = d;
        }

        // Storing a reference to a mutable object is dangerous.

        // Indirect way of storing reference
        public static void setStaticDate2(Date d) {
            Date d2 = d;
            sDate = d;
        }

        public static void setStaticDateArray(Date[] da) {
            sDateArray = da;
        }

        // Cloning the array does not perform deep copy
        public static void setStaticDateArray2(Date[] da) {
            sDateArray = da.clone();
        }

        public static void setStaticValues(HashMap<Integer, String>  values) {
            shm = values;
        }

        public static CharBuffer getStaticBuffer() {
            return sCharBuf;
        }

        public static CharBuffer getStaticBufferDuplicate() {
            return sCharBuf.duplicate();
        }

        public static CharBuffer getStaticBuferWrap() {
            return CharBuffer.wrap(sCharArray);        
        }

        public static void setStaticBuffer(CharBuffer cb) {
            sCharBuf = cb;
        }

        public static void setStaticBufferDuplicate(CharBuffer cb) {
            sCharBuf = cb.duplicate();
        }

        public static void setStaticBufferWrap(char[] cArr) {
            sCharBuf = CharBuffer.wrap(cArr);
        }
    }

    public class Inner {
        // Returning a private mutable object reference attribute is dangerous.

        public Date getDate() {
            return date;
        }

        // Indirect way of returning reference
        public Date getDate2() {
            Date d = date;
            return d;
        }

        public Date[] getDateArray() {
            return dateArray;
        }

        // Cloning the array does not perform deep copy
        public Date[] getDateArray2() {
            return dateArray.clone();
        }

        public HashMap<Integer, String> getValues() {
            return hm;
        }

        public void setDate(Date d) {
            date = d;
        }

        // Storing a reference to a mutable object is dangerous.

        // Indirect way of storing reference
        public void setDate2(Date d) {
            Date d2 = d;
            date = d2;
        }

        public void setDateArray(Date[] da) {
            dateArray = da;
        }

        // Cloning the array does not perform deep copy
        public void setDateArray2(Date[] da) {
            dateArray = da.clone();
        }

        public void setValues(HashMap<Integer, String> values) {
            hm = values;
        }

        public CharBuffer getBuffer() {
            return charBuf;
        }

        public CharBuffer getBufferDuplicate() {
            return charBuf.duplicate();
        }

        public CharBuffer getBuferWrap() {
            return CharBuffer.wrap(charArray);        
        }

        public void setBuffer(CharBuffer cb) {
            charBuf = cb;
        }

        public void setBufferDuplicate(CharBuffer cb) {
            charBuf = cb.duplicate();
        }

        public void setBufferWrap(char[] cArr) {
            charBuf = CharBuffer.wrap(cArr);
        }
    }
}
