package xss;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TaintedCookies extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");

        Cookie cookie = new Cookie("foo", request.getParameter("foo"));
        PrintWriter out = response.getWriter();
        response.addCookie(cookie);

        out.println("<html><body><p>Hello</p></body></html>");
    }
}
