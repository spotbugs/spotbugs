package suppress.custom;

import javax.inject.Inject;

public class ExampleWithUnusedFields {
    @SuppressFBWarnings("XYZ") // Expecting US_USELESS_SUPPRESSION_ON_FIELD and UUF_UNUSED_FIELD here
    private String uselessSuppression;

    @Inject
    private String injected; // No bug expected here because there's an annotation

    @SuppressFBWarnings("XYZ") // Expecting US_USELESS_SUPPRESSION_ON_FIELD here
    @Inject
    private String injectedAndUselessSuppression;

    private String unused; // Expecting UUF_UNUSED_FIELD

    @SuppressFBWarnings("UUF_UNUSED_FIELD")
    private String unusedSuppressed;
}
