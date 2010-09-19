package bugPatterns;

public class FI_EXPLICIT_INVOCATION {

    /**
     * any can't be declared as be Object, since finalize is protected
     */
    void bug(FI_EXPLICIT_INVOCATION any) throws Throwable {
        any.finalize();
    }

}
