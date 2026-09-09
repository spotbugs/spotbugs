package ghIssues;

public class Issue744 {


    public double testParsingBad3(String value) {
        return new Double(value).doubleValue();
    }

    public double testParsingBad3a(String value) {
        return Double.valueOf(value).doubleValue();
    }

    public double testParsingGood3(String value) {
        return Double.parseDouble(value);
    }

    public float testParsingBad4(String value) {
        return new Float(value).floatValue();
    }

    public float testParsingBad4a(String value) {
        return Float.valueOf(value).floatValue();
    }

    public float testParsingGood4(String value) {
        return Float.parseFloat(value);
    }
}
