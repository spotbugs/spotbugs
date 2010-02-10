package edu.umd.cs.findbugs.flybush;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FlybushDataClearer {
    public static void main(String[] args) throws IOException {
        System.out.println("Are you sure you want to clear all data on theflybush.appspot.com? (y/n) ");
        if (System.in.read() != 'y')
            return;
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
            if (line == null) break;
            System.out.println(line);
        }
        conn.disconnect();
    }
}
