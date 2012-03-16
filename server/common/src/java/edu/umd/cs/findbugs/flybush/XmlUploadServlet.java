package edu.umd.cs.findbugs.flybush;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;

public class XmlUploadServlet extends AbstractFlybushServlet {
    private static final Logger LOGGER =
            Logger.getLogger(FileUpload.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().equals("/upload")) {
            resp.setStatus(200);
            printForm(getPersistenceManager(), resp);

        } else if (req.getRequestURI().equals("/create-default-versions")) {
            PersistenceManager pm = getPersistenceManager();
            DbPluginUpdateXml update = persistenceHelper.createPluginUpdateXml();
            update.setChannel("channel");
            update.setDate(new Date());
            update.setMessage("message");
            update.setPluginId("com.example.plugin");
            update.setReleaseDate(new Date());
            update.setUrl("http://url");
            update.setUser("keithl@gmail.com");
            update.setVersion("1.0");
            pm.currentTransaction().begin();
            try {
                pm.makePersistent(update);
                pm.currentTransaction().commit();
            } finally {
                if (pm.currentTransaction().isActive())
                    pm.currentTransaction().rollback();
            }
            setResponse(resp, 200, "OK thanks!");
        } else {
            super.doGet(req, resp);
        }
    }



    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        try {
            ServletFileUpload upload = new ServletFileUpload();

            FileItemIterator iterator = upload.getItemIterator(req);
            boolean stored = false;
            while (iterator.hasNext()) {
                FileItemStream item = iterator.next();
                InputStream stream = item.openStream();

                if (item.isFormField()) {
                    LOGGER.info("Got a form field: " + item.getFieldName());
                } else {
                    LOGGER.info("Got an uploaded file: " + item.getFieldName() + ", name = " + item.getName());

                    int len;
                    byte[] buffer = new byte[8192];
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    while ((len = stream.read(buffer, 0, buffer.length)) != -1) {
                        bout.write(buffer, 0, len);
                    }
                    byte[] array = bout.toByteArray();
                    if (array.length == 0)
                        throw new RuntimeException("No data received");

                    LOGGER.info("Stored XML file - " + array.length + " bytes");
                    DbPluginUpdateXml xml = persistenceHelper.createPluginUpdateXml(new String(array, "UTF-8"));
                    xml.setDate(new Date());
                    xml.setUser(persistenceHelper.getEmailOfCurrentAppengineUser());
                    Transaction tx = pm.currentTransaction();
                    tx.begin();
                    try {
                        pm.makePersistent(xml);
                        tx.commit();
                        stored = true;
                    } finally {
                        if (tx.isActive())
                            tx.rollback();
                    }
                }
            }
            if (!stored)
                printForm(pm, resp);
        } catch (FileUploadException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void printForm(PersistenceManager pm, HttpServletResponse resp) throws IOException {
        List<DbPluginUpdateXml> xmls = (List<DbPluginUpdateXml>)
                UpdateCheckServlet.getUpdateXmlQueryObj(pm, persistenceHelper).execute();
        String currentXml;
        if (xmls.isEmpty())
            currentXml = "(no update xml has been uploaded yet)";
        else
            currentXml = xmls.get(0).getContents();
        if (currentXml == null)
            currentXml = "(error)";

        resp.setContentType("text/html");
        resp.getWriter().print(
                "<html>\n" +
                        "<head>\n" +
                        "  <title>File Upload</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<h1 style=color:red>THIS FORM IS NOT USED ANYMORE!</h1>\n" +
                        "<p style=color:red>Use the App Engine Data Viewer to add/edit plugin updates.</p>\n" +
                        "  <form method=\"post\" action=\"upload\" enctype=\"multipart/form-data\">\n" +
                        "    <input type=\"file\" name=\"file\" />\n" +
                        "    <input type=\"submit\" value=\"Upload\" />\n" +
                        "  </form>\n" +
                        "<h3>Current XML:</h3>" +
                        "<div style='font-family: monospace; height: 400px; overflow:auto; width: 600px; " +
                        "border: 2px solid gray; background-color: #ddf'>" +
                        StringEscapeUtils.escapeHtml(currentXml).replaceAll("\n", "<br>\n").replaceAll(" ", "&nbsp;")
                        + "</div>" +
                        "</body>\n" +
                        "</html>");
    }
}
