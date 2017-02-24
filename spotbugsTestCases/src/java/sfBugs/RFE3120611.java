package sfBugs;

public class RFE3120611 {

    static String[] data = new String[10];

    {

        for (int i = 0; i < data.length; i++)
            data[i] = Integer.toString(i);
    }

    public static String get(int i) {
        return data[i];
    }

    private RFE3120611() {
    }

}
