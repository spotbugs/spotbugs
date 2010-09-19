package bugPatterns;

public class AVERAGE_COMPUTED_USING_DIVISION {

    int bug(int a[], int anyLow, int anyHigh) {
        int mid = (anyLow + anyHigh) / 2;
        return a[mid];
    }

    long bug(long a[], int anyLow, int anyHigh) {
        int mid = (anyLow + anyHigh) / 2;
        return a[mid];
    }

    Object bug(Object a[], int anyLow, int anyHigh) {
        int mid = (anyLow + anyHigh) / 2;
        return a[mid];
    }

    int notBug(int a[], int anyLow, int anyHigh) {
        int mid = (anyLow + anyHigh) >>> 1;
        return a[mid];
    }
}
