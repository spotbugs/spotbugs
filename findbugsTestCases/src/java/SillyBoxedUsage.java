import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class SillyBoxedUsage {
    @ExpectWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testBad1(int value) {
        return new Integer(value).toString();
    }

    @NoWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testGood1(int value) {
        return Integer.toString(value);
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testBad2(float value) {
        return new Float(value).toString();
    }

    @NoWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testGood2(float value) {
        return Float.toString(value);
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testBad3(double value) {
        return new Double(value).toString();
    }

    @NoWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testGood3(double value) {
        return Double.toString(value);
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testBad(byte b, char c, short s, long j, boolean z) {
        return new Byte(b).toString() + new Character(c).toString() + new Short(s).toString() + new Long(j).toString()
                + new Boolean(z).toString();
    }

    @NoWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String testGood(byte b, char c, short s, long j, boolean z) {
        return Byte.toString(b) + Character.toString(c) + Short.toString(s) + Long.toString(j) + Boolean.toString(z);
    }
    
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public int testParsingBad1(String value) {
        return new Integer(value).intValue();
    }
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public int testParsingBad1a(String value) {
        return Integer.valueOf(value).intValue();
    }
    
    @NoWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public int testParsingGood1(String value) {
        return Integer.parseInt(value);
    }
    
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long testParsingBad2(String value) {
        return new Long(value).longValue();
    }
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long testParsingBad2a(String value) {
        return Long.valueOf(value).longValue();
    }
    
    @NoWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long testParsingGood2(String value) {
        return Long.parseLong(value);
    }
}
