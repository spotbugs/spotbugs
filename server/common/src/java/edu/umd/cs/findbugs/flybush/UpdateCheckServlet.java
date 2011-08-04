package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.Lists;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UpdateCheckServlet extends AbstractFlybushServlet {
    @SuppressWarnings({"unchecked"})
    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        final DbUsageEntry entry = persistenceHelper.createDbUsageEntry();
        entry.setIpAddress(req.getRemoteAddr());
        entry.setCountry(GCountryCodes.get(req.getHeader("X-AppEngine-country")));
        entry.setDate(new Date());
        final List<List<String>> plugins = Lists.newArrayList();
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(req.getInputStream(), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if (qName.equals("findbugs-invocation")) {
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String name = attributes.getQName(i);
                            String value = attributes.getValue(i);
                            if (name.equals("version")) entry.setVersion(value);
                            if (name.equals("app-name")) entry.setAppName(value);
                            if (name.equals("app-version")) entry.setAppVersion(value);
                            if (name.equals("entry-point")) entry.setEntryPoint(value);
                            if (name.equals("os")) entry.setOs(value);
                            if (name.equals("java-version")) entry.setJavaVersion(value);
                            if (name.equals("language")) entry.setLanguage(value);
                            if (name.equals("country")) entry.setLocaleCountry(value);
                            if (name.equals("uuid")) entry.setUuid(value);
                        }
                    } else if (qName.equals("plugin")) {

                        String id = null;
                        String name = null;
                        String version = null;
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String qname = attributes.getQName(i);
                            if (qname.equals("id")) id = attributes.getValue(i);
                            if (qname.equals("name")) name = attributes.getValue(i);
                            if (qname.equals("version")) version = attributes.getValue(i);
                        }
                        if (id != null) {
                            plugins.add(Arrays.asList(id, name, version));
                        }
                    }
                }
            });
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
        if (entry.getVersion() == null || entry.getUuid() == null) {
            LOGGER.warning("Not logging usage data - missing information - plugin=" + entry.getPlugin());
            setResponse(resp, 500, "Missing fields in posted XML - version, uuid");
            return;
        }
        try {
            LOGGER.info(entry.toString());
            LOGGER.info(plugins.toString());
            for (List<String> e : plugins) {
                DbUsageEntry pluginEntry = entry.copy();
                pluginEntry.setPlugin(e.get(0));
                pluginEntry.setPluginName(e.get(1));
                pluginEntry.setPluginVersion(e.get(2));

                pm.currentTransaction().begin();
                pm.makePersistent(pluginEntry);
                pm.currentTransaction().commit();
            }
        } finally {
            if (pm.currentTransaction().isActive())
                pm.currentTransaction().rollback();
        }
        //TODO: unit test me!
        resp.setStatus(200);
        resp.setContentType("text/xml");
        try {
            List<DbPluginUpdateXml> results = (List<DbPluginUpdateXml>) pm.newQuery(
                    "select from " + persistenceHelper.getDbPluginUpdateXmlClassname()
                    + " order by date desc limit 1").execute();
            if (results.size() > 0)
                resp.getWriter().write(results.get(0).getContents());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception while grabbing update check XML from DB", e);
        }
    }
}
