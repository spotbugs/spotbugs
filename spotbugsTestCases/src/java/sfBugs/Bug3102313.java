package sfBugs;

import java.util.Currency;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3102313 {


    public int hashCode() {
        return getCurrencyCode().hashCode();
    }
    @NoWarning("NP")
    public boolean equals(Object o) {
        try {
            return ((Bug3102313) o).getCurrencyCode().equals(getCurrencyCode());
        } catch (Exception e) {
            return false;
        }
    }

    private String getCurrencyCode() {
        return Currency.getInstance(Locale.getDefault()).getCurrencyCode();
    }
}
