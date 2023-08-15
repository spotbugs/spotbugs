public record Issue2369() {
    private static final Issue2369 DEFAULT_A = new Issue2369();
    public static Issue2369 defaultA() {
        return DEFAULT_A;
    }
}