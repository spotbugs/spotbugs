package bugIdeas;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2010_12_06 extends HttpServlet {

    @ExpectWarning("PT_RELATIVE_PATH_TRAVERSAL")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
    
    @ExpectWarning("PT_ABSOLUTE_PATH_TRAVERSAL")
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        String path = request.getParameter("path");
        BufferedReader r = new BufferedReader(new FileReader(path));
        while (true) {
            String txt = r.readLine();
            if (txt == null)
                break;
            out.println(txt);
        }
        out.close();
        r.close();
    }
    
    
    @NoWarning("NP_NONNULL_PARAM_VIOLATION,NP_NULL_PARAM_DEREF_NONVIRTUAL")
    public static void main(String args[]) {
        Comparator<Integer> comparator = (Comparator<Integer>) null;
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<Integer, String>(comparator);
        for (int i = 0; i < 16; i++)
            map.put(i, Integer.toString(i));

        System.out.println(map);

    }

}
