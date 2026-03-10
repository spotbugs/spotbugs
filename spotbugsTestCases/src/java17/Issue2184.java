package ghIssues;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Issue2184 {
    public void test(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        String path = request.getParameter("path");
        BufferedReader r = new BufferedReader(new FileReader("data/" + path));
        while (true) {
            String txt = r.readLine();
            if (txt == null)
                break;
            out.println(txt);
        }
        out.close();
        r.close();
    }
}
