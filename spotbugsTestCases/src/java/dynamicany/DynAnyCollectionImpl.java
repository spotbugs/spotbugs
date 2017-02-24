package dynamicany;

public class DynAnyCollectionImpl extends DynAnyConstructedImpl {
    public DynAnyCollectionImpl() {
        this(null, null, false);
    }

    protected DynAnyCollectionImpl(String orb, String any, boolean copyValue) {
        super(orb, any, copyValue);
    }

}
