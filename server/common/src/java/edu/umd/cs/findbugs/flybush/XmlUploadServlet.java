package edu.umd.cs.findbugs.flybush;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

public class XmlUploadServlet extends AbstractFlybushCloudServlet {
    private static final Logger LOGGER =
            Logger.getLogger(FileUpload.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().equals("/create-default-versions")) {
            PersistenceManager pm = getPersistenceManager();
            DbPluginUpdateXml update = persistenceHelper.createPluginUpdateXml();
            update.setChannel("dev");
            Date date = new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
            update.setDate(date);
            update.setMessage("message");
            update.setPluginId("edu.umd.cs.findbugs.plugins.core");
            update.setReleaseDate(date);
            update.setUrl("http://findbugs.cs.umd.edu");
            update.setUser("findbugs.cs.umd.edu");
            update.setVersion("2.0.4");
            update.setJavaVersion(5);
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
        // TODO Auto-generated method stub
        
    }



   
}
