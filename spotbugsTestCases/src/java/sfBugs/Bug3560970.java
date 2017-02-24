package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3560970 {

    enum ResultCode {
        Approved, Declined, Huh
    }

    enum TransactionStatus {
        DECLINED, SYSTEMERROR, OTHER
    };

    @NoWarning("SF_SWITCH_NO_DEFAULT")
    public void test(ResultCode resultCode) {
        boolean isAuthorised;
        TransactionStatus transactionStatus = TransactionStatus.OTHER;
        switch (resultCode) {
        case Approved:
            isAuthorised = true;
            break;
        case Declined:

            transactionStatus = TransactionStatus.DECLINED;
            // Allow fall through into next section
        default:
            if (transactionStatus != TransactionStatus.DECLINED) {
                transactionStatus = TransactionStatus.SYSTEMERROR;
            }
            isAuthorised = false;
            break;
        }
        System.out.println(isAuthorised);
        System.out.println(transactionStatus);
    }
}
