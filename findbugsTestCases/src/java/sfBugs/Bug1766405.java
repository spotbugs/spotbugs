package sfBugs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Submitted By: Steve Tousignant
 * Summary:
 * 
 * find bug detect a false positive in some situation where you have a string
 * initialized before a for/while loop and concat stuff in the loop. The
 * special case here is that the next time the loop is run the string is
 * reinitialized with an other value and stay this way for the rest of the
 * loop.
 * 
 */
public class Bug1766405 {
    // Should not warn
    public void blah() throws IOException {
        List<Vector> baz = new ArrayList<Vector>();
        Vector<String> vecRow = new Vector<String>();
        vecRow.add("blah");
        BufferedWriter bw = new BufferedWriter(new StringWriter());
        String foo = "foo";
        String strHtml = "<TR>" + "<TD CLASS='Cellule'>" + foo + "</TD>";
        for (int j = 0; j < baz.size(); j++) {
            Vector row = (Vector) baz.get(j);
            if (j > 0) {
                strHtml = "<TR>" + "<TD CLASS='Cellule'>&nbsp;</TD>";
            }
            strHtml += "<TD><INPUT TYPE='CHECKBOX'>" + (String) vecRow.get(0)
                    + "</TD>" + "<TD>" + row.get(1) + "</TD>" + "<TD>"
                    + row.get(2) + "</TD>" + "<TD>" + row.get(3) + "</TD>"
                    + "<TD>" + row.get(4) + "</TD>" + "<TD>" + row.get(5)
                    + "</TD>" + "<TD ALIGN=RIGHT>" + row.get(6) + "</TD>"
                    + "</TR>";
            bw.write(strHtml);
        }
    }

    // Should warn
    public void blah2() throws IOException {
        List<Vector> baz = new ArrayList<Vector>();
        Vector<String> vecRow = new Vector<String>();
        vecRow.add("blah");
        BufferedWriter bw = new BufferedWriter(new StringWriter());
        String foo = "foo";
        String strHtml = "<TR>" + "<TD CLASS='Cellule'>" + foo + "</TD>";
        for (int j = 0; j < baz.size(); j++) {
            Vector row = (Vector) baz.get(j);
            strHtml += "<TD><INPUT TYPE='CHECKBOX'>" + (String) vecRow.get(0)
                    + "</TD>" + "<TD>" + row.get(1) + "</TD>" + "<TD>"
                    + row.get(2) + "</TD>" + "<TD>" + row.get(3) + "</TD>"
                    + "<TD>" + row.get(4) + "</TD>" + "<TD>" + row.get(5)
                    + "</TD>" + "<TD ALIGN=RIGHT>" + row.get(6) + "</TD>"
                    + "</TR>";
            bw.write(strHtml);
        }
    }
    
    // Should warn
    public void blah3() throws IOException {
        List<Vector> baz = new ArrayList<Vector>();
        Vector<String> vecRow = new Vector<String>();
        vecRow.add("blah");
        BufferedWriter bw = new BufferedWriter(new StringWriter());
        String foo = "foo";
        String strHtml = "<TR>" + "<TD CLASS='Cellule'>" + foo + "</TD>";
        for (int j = 0; j < baz.size(); j++) {
            Vector row = (Vector) baz.get(j);
            strHtml += "<TD><INPUT TYPE='CHECKBOX'>" + (String) vecRow.get(0)
                    + "</TD>" + "<TD>" + row.get(1) + "</TD>" + "<TD>"
                    + row.get(2) + "</TD>" + "<TD>" + row.get(3) + "</TD>"
                    + "<TD>" + row.get(4) + "</TD>" + "<TD>" + row.get(5)
                    + "</TD>" + "<TD ALIGN=RIGHT>" + row.get(6) + "</TD>"
                    + "</TR>";
            strHtml = strHtml + "blah";
            bw.write(strHtml);
        }
    }

    // Should not warn
    public void blah4() throws IOException {
        List<Vector> baz = new ArrayList<Vector>();
        Vector<String> vecRow = new Vector<String>();
        vecRow.add("blah");
        BufferedWriter bw = new BufferedWriter(new StringWriter());
        String foo = "foo";
        String strHtml = "<TR>" + "<TD CLASS='Cellule'>" + foo + "</TD>";
        for (int j = 0; j < baz.size(); j++) {
            Vector row = (Vector) baz.get(j);
            strHtml += "<TD><INPUT TYPE='CHECKBOX'>" + (String) vecRow.get(0)
                    + "</TD>" + "<TD>" + row.get(1) + "</TD>" + "<TD>"
                    + row.get(2) + "</TD>" + "<TD>" + row.get(3) + "</TD>"
                    + "<TD>" + row.get(4) + "</TD>" + "<TD>" + row.get(5)
                    + "</TD>" + "<TD ALIGN=RIGHT>" + row.get(6) + "</TD>"
                    + "</TR>";
            System.out.println(strHtml);
            strHtml = "blah";
            bw.write(strHtml);
        }
    }
}
