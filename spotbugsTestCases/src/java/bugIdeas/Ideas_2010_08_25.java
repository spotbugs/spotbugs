package bugIdeas;

import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Ideas_2010_08_25 extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

        String id = request.getParameter("id");

        PrintWriter out = response.getWriter();
        response.setContentType("text/plain");
        out.println("Id is " + id);
    }

}
