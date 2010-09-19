package xss;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StringBufferCrossSiteScripting extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        String name = request.getParameter("firstName");

        PrintWriter out = response.getWriter();

        out.println("<p>Hi" + name);
        out.println("<p>Please come back" + name);
        out.close();
    }
}
