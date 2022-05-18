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

        @ExpectWarning("MS")
        public static Date getStaticDate() {
            return sDate;
        }

        @ExpectWarning("MS") // Indirect way of returning reference
        public static Date getStaticDate2() {
            Date d = sDate;
            return d;
        }

        @ExpectWarning("MS")
        public static Date[] getStaticDateArray() {
            return sDateArray;
        }

        @ExpectWarning("MS") // Cloning the array does not perform deep copy
        public static Date[] getStaticDateArray2() {
            return sDateArray.clone();
        }

        @ExpectWarning("MS")
        public static HashMap<Integer, String> getStaticValues() {
            return shm;
        }

        @ExpectWarning("MS")
        public static void setStaticDate(Date d) {
            sDate = d;
        }

        // Storing a reference to a mutable object is dangerous.

        @ExpectWarning("MS") // Indirect way of storing reference
        public static void setStaticDate2(Date d) {
            Date d2 = d;
            sDate = d;
        }

        @ExpectWarning("MS")
        public static void setStaticDateArray(Date[] da) {
            sDateArray = da;
        }

        @ExpectWarning("MS") // Cloning the array does not perform deep copy
        public static void setStaticDateArray2(Date[] da) {
            sDateArray = da.clone();
        }

        @ExpectWarning("MS")
        public static void setStaticValues(HashMap<Integer, String>  values) {
            shm = values;
        }

        @ExpectWarning("MS")
        public static CharBuffer getStaticBuffer() {
            return sCharBuf;
        }

        @ExpectWarning("MS")
        public static CharBuffer getStaticBufferDuplicate() {
            return sCharBuf.duplicate();
        }

        @ExpectWarning("MS")
        public static CharBuffer getStaticBuferWrap() {
            return CharBuffer.wrap(sCharArray);        
        }

        @ExpectWarning("MS")
        public static void setStaticBuffer(CharBuffer cb) {
            sCharBuf = cb;
        }

        @ExpectWarning("MS")
        public static void setStaticBufferDuplicate(CharBuffer cb) {
            sCharBuf = cb.duplicate();
        }

        @ExpectWarning("MS")
        public static void setStaticBufferWrap(char[] cArr) {
            sCharBuf = CharBuffer.wrap(cArr);
        }
    }

    public class Inner {
        // Returning a private mutable object reference attribute is dangerous.

        @ExpectWarning("EI")
        public Date getDate() {
            return date;
        }

        @ExpectWarning("EI") // Indirect way of returning reference
        public Date getDate2() {
            Date d = date;
            return d;
        }

        @ExpectWarning("EI")
        public Date[] getDateArray() {
            return dateArray;
        }

        @ExpectWarning("EI") // Cloning the array does not perform deep copy
        public Date[] getDateArray2() {
            return dateArray.clone();
        }

        @ExpectWarning("EI")
        public HashMap<Integer, String> getValues() {
            return hm;
        }

        @ExpectWarning("EI2")
        public void setDate(Date d) {
            date = d;
        }

        // Storing a reference to a mutable object is dangerous.

        @ExpectWarning("EI2") // Indirect way of storing reference
        public void setDate2(Date d) {
            Date d2 = d;
            date = d2;
        }

        @ExpectWarning("EI2")
        public void setDateArray(Date[] da) {
            dateArray = da;
        }

        @ExpectWarning("EI2") // Cloning the array does not perform deep copy
        public void setDateArray2(Date[] da) {
            dateArray = da.clone();
        }

        @ExpectWarning("EI2")
        public void setValues(HashMap<Integer, String> values) {
            hm = values;
        }

        @ExpectWarning("EI")
        public CharBuffer getBuffer() {
            return charBuf;
        }

        @ExpectWarning("EI")
        public CharBuffer getBufferDuplicate() {
            return charBuf.duplicate();
        }

        @ExpectWarning("EI")
        public CharBuffer getBuferWrap() {
            return CharBuffer.wrap(charArray);        
        }

        @ExpectWarning("EI2")
        public void setBuffer(CharBuffer cb) {
            charBuf = cb;
        }

        @ExpectWarning("EI2")
        public void setBufferDuplicate(CharBuffer cb) {
            charBuf = cb.duplicate();
        }

        @ExpectWarning("EI2")
        public void setBufferWrap(char[] cArr) {
            charBuf = CharBuffer.wrap(cArr);
        }
    }
}
