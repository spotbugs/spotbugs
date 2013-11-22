package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUpload;

public class XmlUploadServlet extends AbstractFlybushUpdateServlet {
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
            update.setPluginId("edu.umd.cs.findbugs.plugins.test");
            update.setReleaseDate(date);
            update.setUrl("http://findbugs.cs.umd.edu");
            update.setUser("findbugs.cs.umd.edu");
            update.setVersion("2.0.3");
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
