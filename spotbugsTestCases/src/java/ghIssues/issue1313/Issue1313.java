package ghIssues.issue1313;

/**
 * Manual test snippet for issue 1313.
 */
public class Issue1313 {

    enum TestEnum {
        TEST_ENUM_VALUE1, TEST_ENUM_VALUE2,
    }

    static void println(TestEnum testEnum) {
        switch (testEnum) {
        case TEST_ENUM_VALUE1:
            System.out.println("TEST_ENUM_VALUE1");
            break;
        case TEST_ENUM_VALUE2:
            System.out.println("TEST_ENUM_VALUE2");
            break;
        default:
            System.out.println("unknown enum value");
        }
    }
}
