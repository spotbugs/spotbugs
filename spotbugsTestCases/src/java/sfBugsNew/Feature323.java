package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Feature323 {
    private int[] array = new int[] {1,2,3,4};
    private static final String TEST_STRING = " test string ";
    
    @ExpectWarning("RANGE_ARRAY_INDEX")
    public int getValue() {
        return array[4];
    }
    
    @NoWarning("RANGE_ARRAY_INDEX")
    public int getValue3() {
        return array[3];
    }

    @ExpectWarning("RANGE_ARRAY_INDEX")
    public int[] getValueLocal(boolean flag) {
        int[] array = new int[5];
        if(flag) {
            array = new int[10];
            array[6] = 1;
            return array;
        } else {
            array[6] = 2;
            return array;
        }
    }

    @ExpectWarning("RANGE_ARRAY_LENGTH")
    public int[] copy() {
        int[] newArray = new int[4];
        System.arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }
    
    @NoWarning("RANGE_ARRAY_LENGTH")
    public int[] copyCorrect() {
        int[] newArray = new int[4];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray = new int[5];
        System.arraycopy(newArray, 1, array, 0, array.length);
        System.arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }
    @ExpectWarning("RANGE_ARRAY_LENGTH")
    public String str() {
        char[] chars = new char[] {'b','u','g'};
        return new String(chars, chars.length, chars.length);
    }
    
    @ExpectWarning("RANGE_ARRAY_LENGTH")
    public String str2() {
        char[] chars = new char[] {'b','u','g'};
        return new String(chars, chars.length+1, chars.length);
    }
    
    @NoWarning("RANGE")
    public String strCorrect() {
        char[] chars = new char[] {'b','u','g'};
        return new String(chars, 0, chars.length);
    }
    
    @NoWarning("RANGE")
    public String strEmpty() {
        return new String(new char[0], 0, 0);
    }
    
    @ExpectWarning("RANGE_STRING_INDEX")
    public String trimSubstring() {
        return TEST_STRING.trim().substring(TEST_STRING.length());
    }
    
    @NoWarning("RANGE_STRING_INDEX")
    public String substringCorrect() {
        return TEST_STRING.substring(TEST_STRING.length())
                + TEST_STRING.substring(TEST_STRING.length()-1)
                +  TEST_STRING.substring(1, TEST_STRING.length())
                +  TEST_STRING.substring(1, 2);
    }
    
    @ExpectWarning("RANGE_STRING_INDEX")
    public String substringIncorrect() {
        return TEST_STRING.substring(TEST_STRING.length()+1);
    }
    @ExpectWarning("RANGE_STRING_INDEX")
    public String substringIncorrect2() {
        return TEST_STRING.substring(1, TEST_STRING.length()+1);
    }
    
    @ExpectWarning("RANGE_STRING_INDEX")
    public String substringIncorrect3() {
        return TEST_STRING.substring(2, 1);
    }
    @NoWarning("RANGE_STRING_INDEX")
    public char charAtCorrect() {
        return TEST_STRING.charAt(TEST_STRING.length()-1);
    }
    
    @ExpectWarning("RANGE_STRING_INDEX")
    public char charAtIncorrect() {
        return TEST_STRING.charAt(TEST_STRING.length());
    }
    
   
}
