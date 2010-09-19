package edu.umd.cs.findbugs.flybush.appengine;

public class FlybushDataClearer {
    public static void main(String[] args) throws IOException {
        System.out.println("Are you sure you want to clear all data on theflybush.appspot.com? (y/n) ");
        String typedLine = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (!typedLine.equals("y"))
            return;
        while (true) {
            if (!clearAllData())
                break;
        }
    }

    private static boolean clearAllData() throws IOException {
        URL url = new URL("http://theflybush.appspot.com/clear-all-data");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().close();
        conn.connect();
        System.out.println(conn.getResponseCode() + " " + conn.getResponseMessage());
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            System.out.println(line);
            if (line.equals("Deleted 0 entities"))
                return false;
        }
        conn.disconnect();
        return true;
    }
}
