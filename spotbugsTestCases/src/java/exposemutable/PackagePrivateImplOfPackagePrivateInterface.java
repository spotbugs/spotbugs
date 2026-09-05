package exposemutable;

/**
 * Package-private class implementing a package-private interface.
 * Should NOT be flagged because the interface is not externally visible.
 */
class PackagePrivateImplOfPackagePrivateInterface implements PackagePrivateInterface {
    private byte[] secret = { 4, 5, 6 };

    @Override
    public byte[] getSecret() {
        return secret; // No bug - interface is package-private
    }
}
