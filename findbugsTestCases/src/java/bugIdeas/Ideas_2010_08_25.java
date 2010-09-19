package bugIdeas;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Ideas_2010_08_25 extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

        String id = request.getParameter("id");

        PrintWriter out = response.getWriter();
        response.setContentType("text/plain");
        out.println("Id is " + id);
    }

}
