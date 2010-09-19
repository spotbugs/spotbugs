package npe;

// Demonstrates a bug in a prototype of the guaranteed null dereference detector
// Inspired by a situation where the bug exhibited in java.awt.Color, but
// substantially cut down.

public class Color {
    float[] fvalue;

    public Color(float components[]) {
        int result = 0;
        if (components == null)
            result = 1;
        Object x = new Object();
        int n = 5;
        fvalue = new float[n];
        for (int i = 0; i < n; i++) {
            result++;
            if (components[i] < 0.0) {
                result += x.hashCode();
            } else {
                float f = components[i];
                fvalue[i] = f;
            }
        }

    }

}
