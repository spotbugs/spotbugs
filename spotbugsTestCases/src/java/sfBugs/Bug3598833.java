package sfBugs;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jspecify.annotations.NonNull;

public class Bug3598833 {

    public static @NonNull BigDecimal numberToBigDecimal(@NonNull Number number) {
        BigDecimal bigDecimal = null;

        if (number instanceof BigDecimal)
            bigDecimal = (BigDecimal) number;
        else {
            if (!(number instanceof BigInteger) && !(number instanceof Byte) && !(number instanceof Short)
                    && !(number instanceof Integer) && !(number instanceof Long))
                throw new ClassCastException(number.getClass().getName());

            if (number.intValue() == 0)
                bigDecimal = BigDecimal.ZERO;
            else if (number.intValue() == 1)
                bigDecimal = BigDecimal.ONE;
            else if (number.intValue() == 10)
                bigDecimal = BigDecimal.TEN;
            else if (number instanceof BigInteger)
                bigDecimal = new BigDecimal((BigInteger) number);
            else if (number instanceof Byte)
                bigDecimal = new BigDecimal(number.byteValue());
            else if (number instanceof Short)
                bigDecimal = new BigDecimal(number.shortValue());
            else if (number instanceof Integer)
                bigDecimal = new BigDecimal(number.intValue());
            else if (number instanceof Long)
                bigDecimal = new BigDecimal(number.longValue());
        }

        return bigDecimal;
    }

    public static @NonNull BigDecimal numberToBigDecimal2(@NonNull Number number) {

        if (number instanceof BigDecimal)
            return (BigDecimal) number;
        else if (!(number instanceof BigInteger) && !(number instanceof Byte) && !(number instanceof Short)
                && !(number instanceof Integer) && !(number instanceof Long))
            throw new ClassCastException(number.getClass().getName());
        else if (number.intValue() == 0)
            return BigDecimal.ZERO;
        else if (number.intValue() == 1)
            return BigDecimal.ONE;
        else if (number.intValue() == 10)
            return BigDecimal.TEN;
        else if (number instanceof BigInteger)
            return new BigDecimal((BigInteger) number);
        else if (number instanceof Byte)
            return new BigDecimal(number.byteValue());
        else if (number instanceof Short)
            return new BigDecimal(number.shortValue());
        else if (number instanceof Integer)
            return new BigDecimal(number.intValue());
        else if (number instanceof Long)
            return new BigDecimal(number.longValue());
        else
            throw new AssertionError(number.getClass().getName());
    }

}
