package sfBugs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Bug3432911 {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        InputStream msgInputStream = null;
        BufferedReader r = null;
        InputStreamReader isr = null;
        try {
            msgInputStream = request.getInputStream();
            isr = new InputStreamReader(msgInputStream);
            r = new BufferedReader(isr);
        } catch (Exception ex) {

        }
    }

}
