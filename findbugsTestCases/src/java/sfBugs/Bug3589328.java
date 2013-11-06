package sfBugs;

import java.net.URI;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3589328 {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public static class UriToString implements Function<URI, String> {

        @NoWarning(value="NP")
        public String apply(@Nonnull URI input) {
            return input.toString();
        }
    }


}
 