package xss;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
