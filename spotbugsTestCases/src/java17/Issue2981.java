/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2981">#2981</a>
 */
record Issue2981(int firstRow, int lastRow) {
    private static final Issue2981 NONE = new Issue2981(0, -1);

    public static Issue2981 none() {
        return NONE;
    }
}
