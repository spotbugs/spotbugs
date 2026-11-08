package inefficient;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * Test cases for IATS_INEFFICIENT_APPEND_TO_STRING detector.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/704">Issue #704</a>
 */
class InefficientAppendableToStringDemo {

    // StringBuilder tests

    @ExpectWarning("IATS")
    void appendStringBuilderToString_bad(StringBuilder sb, StringBuilder other) {
        sb.append(other.toString());
    }

    @NoWarning("IATS")
    void appendStringBuilder_good(StringBuilder sb, StringBuilder other) {
        sb.append(other);
    }

    @NoWarning("IATS")
    void appendString_good(StringBuilder sb, String str) {
        sb.append(str);
    }

    // StringBuffer tests

    @ExpectWarning("IATS")
    void appendStringBufferToString_bad(StringBuffer sb, StringBuffer other) {
        sb.append(other.toString());
    }

    @NoWarning("IATS")
    void appendStringBuffer_good(StringBuffer sb, StringBuffer other) {
        sb.append(other);
    }

    // CharSequence interface tests

    @ExpectWarning("IATS")
    void appendCharSequenceToString_bad(StringBuilder sb, CharSequence cs) {
        sb.append(cs.toString());
    }

    @NoWarning("IATS")
    void appendCharSequence_good(StringBuilder sb, CharSequence cs) {
        sb.append(cs);
    }

    // Mixed scenarios

    @ExpectWarning("IATS")
    void appendInChain_bad(StringBuilder sb, StringBuilder other) {
        sb.append("prefix").append(other.toString()).append("suffix");
    }

    @NoWarning("IATS")
    void appendInChain_good(StringBuilder sb, StringBuilder other) {
        sb.append("prefix").append(other).append("suffix");
    }

    // Non-CharSequence toString - should NOT warn

    @NoWarning("IATS")
    void appendObjectToString_ok(StringBuilder sb, Object obj) {
        sb.append(obj.toString());
    }

    @NoWarning("IATS")
    void appendIntegerToString_ok(StringBuilder sb, Integer num) {
        sb.append(num.toString());
    }

    // Direct string literal - should NOT warn

    @NoWarning("IATS")
    void appendStringLiteral_ok(StringBuilder sb) {
        sb.append("literal");
    }

    // Variable assignment pattern

    @NoWarning("IATS")
    String convertToString_ok(StringBuilder sb) {
        return sb.toString();
    }
}
