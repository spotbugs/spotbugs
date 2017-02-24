package sfBugsNew;

public class Bug1263 {

    // Based on com.daffodilwoods.daffodildb.utils.field.FieldLiteral
    public void crash(String msg) {
        new java.math.BigDecimal(Double.POSITIVE_INFINITY);
        if ("Infinite or NaN".equals(msg)) {}
    }
}
