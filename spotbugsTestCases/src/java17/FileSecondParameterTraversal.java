package ghIssues;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class FileSecondParameterTraversal {

    public void direct(HttpServletRequest request) throws ServletException, IOException {
        new File(request.getParameter("path")).delete();
    }

    public void underBaseString(HttpServletRequest request) throws ServletException, IOException {
        new File("data", request.getParameter("path")).delete();
    }

    public void underBaseFile(HttpServletRequest request) throws ServletException, IOException {
        new File(new File("data"), request.getParameter("path")).delete();
    }
}
