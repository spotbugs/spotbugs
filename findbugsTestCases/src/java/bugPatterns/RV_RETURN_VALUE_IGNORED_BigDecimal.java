package bugPatterns;

import java.math.BigDecimal;

public class RV_RETURN_VALUE_IGNORED_BigDecimal {

    void bug(BigDecimal any1, BigDecimal any2) {
        any1.add(any2);
    }

    void notBug(BigDecimal any1, BigDecimal any2) {
        BigDecimal any3 = any1.add(any2);
    }

    void bug(BigDecimal any1) {
        any1.abs();
    }

    void notBug(BigDecimal any1) {
        BigDecimal any2 = any1.abs();
    }

    void bug2(BigDecimal any1, int anyInt) {
        any1.movePointLeft(anyInt);
    }

    void bug3(BigDecimal any1, int anyInt) {
        any1.setScale(anyInt);
    }
}
