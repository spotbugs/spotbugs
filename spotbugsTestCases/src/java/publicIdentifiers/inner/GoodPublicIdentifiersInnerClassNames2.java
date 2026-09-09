package publicIdentifiers.inner;

import java.util.Base64;

public class GoodPublicIdentifiersInnerClassNames2 {
    String baseEncodedField;
    
    public GoodPublicIdentifiersInnerClassNames2(String value) {
        this.baseEncodedField = Base64.getEncoder().encodeToString(value.getBytes());
    }

}
