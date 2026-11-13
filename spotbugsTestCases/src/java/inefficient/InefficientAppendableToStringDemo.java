package inefficient;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Test cases for IATS_INEFFICIENT_APPEND_TO_STRING detector.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/704">Issue #704</a>
 */
class InefficientAppendableToStringDemo {

    // ==================== BAD PATTERNS (should trigger warnings) ====================

    // StringBuilder tests - BAD

    void appendStringBuilderToString_bad(StringBuilder sb, StringBuilder other) {
        sb.append(other.toString());
    }

    // StringBuffer tests - BAD

    void appendStringBufferToString_bad(StringBuffer sb, StringBuffer other) {
        sb.append(other.toString());
    }

    // CharSequence interface tests - BAD

    void appendCharSequenceToString_bad(StringBuilder sb, CharSequence cs) {
        sb.append(cs.toString());
    }

    // Chained calls - BAD

    void appendInChain_bad(StringBuilder sb, StringBuilder other) {
        sb.append("prefix").append(other.toString()).append("suffix");
    }

    // Lambda examples - BAD

    void appendInLambda_bad(StringBuilder sb, StringBuilder other) {
        Runnable r = () -> sb.append(other.toString());
        r.run();
    }

    void appendWithConsumer_bad(StringBuilder sb, StringBuilder other) {
        Consumer<StringBuilder> consumer = builder -> builder.append(other.toString());
        consumer.accept(sb);
    }

    void appendWithSupplier_bad(StringBuilder sb, StringBuilder other) {
        Supplier<StringBuilder> supplier = () -> sb.append(other.toString());
        supplier.get();
    }

    String appendWithFunction_bad(StringBuilder sb, StringBuilder other) {
        Function<StringBuilder, String> func = builder -> {
            builder.append(other.toString());
            return builder.toString();
        };
        return func.apply(sb);
    }

    // ==================== GOOD PATTERNS (should NOT trigger warnings) ====================

    // StringBuilder tests - GOOD

    void appendStringBuilder_good(StringBuilder sb, StringBuilder other) {
        sb.append(other);
    }

    void appendString_good(StringBuilder sb, String str) {
        sb.append(str);
    }

    // StringBuffer tests - GOOD

    void appendStringBuffer_good(StringBuffer sb, StringBuffer other) {
        sb.append(other);
    }

    // CharSequence interface tests - GOOD

    void appendCharSequence_good(StringBuilder sb, CharSequence cs) {
        sb.append(cs);
    }

    // Chained calls - GOOD

    void appendInChain_good(StringBuilder sb, StringBuilder other) {
        sb.append("prefix").append(other).append("suffix");
    }

    // Lambda examples - GOOD

    void appendInLambda_good(StringBuilder sb, StringBuilder other) {
        Runnable r = () -> sb.append(other);
        r.run();
    }

    void appendWithConsumer_good(StringBuilder sb, StringBuilder other) {
        Consumer<StringBuilder> consumer = builder -> builder.append(other);
        consumer.accept(sb);
    }

    // Non-CharSequence toString - should NOT warn (no alternative)

    void appendObjectToString_ok(StringBuilder sb, Object obj) {
        sb.append(obj.toString());
    }

    void appendIntegerToString_ok(StringBuilder sb, Integer num) {
        sb.append(num.toString());
    }

    // Direct string literal - should NOT warn

    void appendStringLiteral_ok(StringBuilder sb) {
        sb.append("literal");
    }

    // Variable assignment pattern - should NOT warn

    String convertToString_ok(StringBuilder sb) {
        return sb.toString();
    }

    // Method reference patterns - GOOD

    void appendWithMethodRef_good(StringBuilder sb, StringBuilder other) {
        Consumer<CharSequence> consumer = sb::append;
        consumer.accept(other);
    }
}
