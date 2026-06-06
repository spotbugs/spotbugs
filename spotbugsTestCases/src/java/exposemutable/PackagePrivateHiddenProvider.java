package exposemutable;

class PackagePrivateHiddenProvider implements PackagePrivateImplProvider {
    private byte[] data = { 1, 2, 3 };

    @Override
    public byte[] getData() {
        return data;
    }
}
