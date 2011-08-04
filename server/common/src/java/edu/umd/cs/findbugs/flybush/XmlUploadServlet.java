package edu.umd.cs.findbugs.flybush;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class XmlUploadServlet extends AbstractFlybushServlet {
    private static final Logger LOGGER =
            Logger.getLogger(FileUpload.class.getName());

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
                throw new RuntimeException("Did not receive file!");
            else
                setResponse(resp, 200, "Success!");
        } catch (FileUploadException ex) {
            throw new RuntimeException(ex);
        }
    }
}
