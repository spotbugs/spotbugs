package exposemutable;

import java.util.Date;

/**
 * Package-private class implementing a public interface but with correct defensive copies.
 * Should NOT be flagged.
 */
class PackagePrivateImplNoBug implements PackagePrivateImplProvider {
    private byte[] data = { 1, 2, 3 };
    private Date date = new Date();

    @Override
    public byte[] getData() {
        return data.clone(); // Defensive copy - no bug
    }

    // This method is NOT declared in the interface, so even though the class is package-private,
    // it is not reachable from outside via the super-type. Should NOT be flagged.
    public Date getDateNotInInterface() {
        return date;
    }
}
