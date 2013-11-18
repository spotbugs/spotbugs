package edu.umd.cs.findbugs.flybush;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.Lists;

public class UpdateCheckServlet extends AbstractFlybushUpdateServlet {

    public static final String PLUGIN_RELEASE_DATE_FMT = "MM/dd/yyyy hh:mm aa z";
    private static final Pattern RC_REGEX = Pattern.compile("[\\d\\.]+-rc\\d+");
    private static final Pattern DEV_REGEX = Pattern.compile("[\\d\\.]+-dev-\\d+");
    private static final Pattern BETA_REGEX = Pattern.compile("[\\d\\.]+-beta\\d+");
    private static final Pattern ALPHA_REGEX = Pattern.compile("[\\d\\.]+-alpha\\d+");
    private static final Pattern STABLE_REGEX = Pattern.compile("[\\d\\.]+");

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(200);
        resp.setContentType("text/plain");
        resp.getWriter().println("OK");
    }
    
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
            parsePluginsXml(req, entry, plugins);
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

        List<DbUsageEntry> entries = commitEntries(pm, entry, plugins);

        resp.setStatus(200);
        resp.setContentType("text/xml");

        /*
        <?xml version="1.0" encoding="UTF-8"?>
        <fb-plugin-updates>
            <plugin id="edu.umd.cs.findbugs.plugins.core">
                <release
                        date="12/21/2011 10:00 am EST"
                        version="2.0.0"
                        url="http://findbugs.cs.umd.edu/">
                   <message>FindBugs 2.0.0 has been released</message>
                </release>
            </plugin>
        </fb-plugin-updates>
        */

        try {
            writeResponseXml(pm, resp, entries);
        } catch (XMLStreamException e) {
            LOGGER.log(Level.WARNING, "Exception while writing XML", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void writeResponseXml(PersistenceManager pm, HttpServletResponse resp, List<DbUsageEntry> entries)
            throws XMLStreamException, IOException {
        StringWriter w = new StringWriter();
        XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(w);
        writer = makeIndentingStreamWriterIfPossible(writer);
        writer.writeStartDocument();
        writer.writeStartElement("fb-plugin-updates");
        Query query = pm.newQuery("select from " + persistenceHelper.getDbPluginUpdateXmlClassname()
                + " order by pluginId ascending, releaseDate descending");
        List<DbPluginUpdateXml> results = (List<DbPluginUpdateXml>) query.execute();
        if (!results.isEmpty()) {
            DateFormat df = new SimpleDateFormat(PLUGIN_RELEASE_DATE_FMT, Locale.ENGLISH);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String oldPluginId = null;
            boolean wrotePlugin = false;
            for (DbPluginUpdateXml result : results) {
                String pluginId = result.getPluginId();
                if (pluginId == null)
                    continue;
                
                // only include this plugin if the user has it
                DbUsageEntry found = findSubmittedPluginUsageEntry(entries, pluginId);
                if (found == null)
                    continue;
                String channel = null;
//                if (found != null)
                channel = findOrDetectChannel(found);

                String javaVersion = found.getJavaVersion();
                if (javaVersion != null && javaVersion.startsWith("1.")) {
                    int userJavaVersion = Integer.parseInt(javaVersion.substring(2));
                    if (userJavaVersion < result.getJavaVersion())
                        continue;
                }
                try {
                    oldPluginId = writePluginUpdate(writer, df, oldPluginId, result, channel);
                    if (oldPluginId != null)
                        wrotePlugin = true;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Can't write plugin update " + result, e);
                }
            }
            if (wrotePlugin)
                writer.writeEndElement(); // plugin
        }
        writer.writeEndElement(); // fb-plugin-updates
        writer.writeEndDocument();
        writer.close();
        resp.getWriter().println(w.toString());
    }

    private String findOrDetectChannel(DbUsageEntry found) {
        String userChannel = found.getPluginChannel();
        if (userChannel != null) {
            return userChannel;
        } else {
            String pluginVersion = found.getPluginVersion();
            return getChannel(pluginVersion);
        }
    }

    /**
     * @param pluginVersion
     * @return
     */
    public String getChannel(String pluginVersion) {
        String channel = null;
        if (pluginVersion != null) {
            if (STABLE_REGEX.matcher(pluginVersion).matches()) channel = "stable";
            if (RC_REGEX.matcher(pluginVersion).matches()) channel = "rc";
            if (BETA_REGEX.matcher(pluginVersion).matches()) channel = "beta";
            if (ALPHA_REGEX.matcher(pluginVersion).matches()) channel = "alpha";
            if (DEV_REGEX.matcher(pluginVersion).matches()) channel = "dev";
            if (channel == null)
                LOGGER.warning("version does not match any regex: " + pluginVersion);
            else
                LOGGER.info("version matches! " + pluginVersion  + " is " + channel);
        }
        return channel;
    }

    private DbUsageEntry findSubmittedPluginUsageEntry(List<DbUsageEntry> entries, String pluginId) {
        DbUsageEntry found = null;
        for (DbUsageEntry entry : entries) {
            if (pluginId.equals(entry.getPlugin())) {
                found = entry;
            }
        }
        return found;
    }

    private XMLStreamWriter makeIndentingStreamWriterIfPossible(XMLStreamWriter writer) {
        try {
            Class<?> cls = Class.forName("com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter");
            writer = (XMLStreamWriter) cls.getConstructor(XMLStreamWriter.class).newInstance(writer);
        } catch (Throwable t) {
            // ignore
        }
        return writer;
    }

    private String writePluginUpdate(XMLStreamWriter writer, DateFormat df, String oldPluginId,
                                     DbPluginUpdateXml result, String userChannel) throws XMLStreamException {
        String newPluginId = result.getPluginId();
        if (newPluginId == null)
            return null;
        String channel = result.getChannel();
        if (!isChannelRelevant(userChannel, channel))
            return oldPluginId;

        boolean newPlugin = !newPluginId.equals(oldPluginId);
        if (newPlugin) {
            if (oldPluginId != null)
                writer.writeEndElement(); // plugin

            writer.writeStartElement("plugin");
            writer.writeAttribute("id", newPluginId);
        }
        oldPluginId = newPluginId;

        writer.writeStartElement("release");
        if (channel != null) writer.writeAttribute("channel", channel);
        Date releaseDate = result.getReleaseDate();
        if (releaseDate != null) writer.writeAttribute("date", df.format(releaseDate));
        String version = result.getVersion();
        if (version != null) writer.writeAttribute("version", version);
        writer.writeAttribute("url", result.getUrl());

        String message = result.getMessage();

        if (message != null) {
            writer.writeStartElement("message");
            writer.writeCData(message);
            writer.writeEndElement(); // message
        }

        writer.writeEndElement(); // release
        return oldPluginId;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isChannelRelevant(String userChannel, String channel) {
        if (userChannel == null || userChannel.equals(channel)) 
            return true;
        if (channel.equals("stable") && asList("dev", "alpha", "beta", "rc").contains(userChannel))
            return true;
        if (channel.equals("rc") && asList("dev", "alpha", "beta").contains(userChannel))
            return true;
        if (channel.equals("beta") && asList("dev", "alpha").contains(userChannel))
            return true;
        if (channel.equals("alpha") && asList("dev").contains(userChannel))
            return true;

        return false;
    }

    private List<DbUsageEntry> commitEntries(PersistenceManager pm, DbUsageEntry entry, List<List<String>> plugins) {
        List<DbUsageEntry> entries = Lists.newArrayList();
        try {
            LOGGER.info(entry.toString());
            LOGGER.info(plugins.toString());
            for (List<String> e : plugins) {
                DbUsageEntry pluginEntry = entry.copy();
                pluginEntry.setPlugin(e.get(0));
                pluginEntry.setPluginName(e.get(1));
                pluginEntry.setPluginVersion(e.get(2));
                pluginEntry.setPluginChannel(e.get(3));

                pm.currentTransaction().begin();
                pm.makePersistent(pluginEntry);
                pm.currentTransaction().commit();
                entries.add(pluginEntry);
            }
        } finally {
            if (pm.currentTransaction().isActive())
                pm.currentTransaction().rollback();
        }
        return entries;
    }

    private void parsePluginsXml(HttpServletRequest req, final DbUsageEntry entry, final List<List<String>> plugins)
            throws SAXException, IOException, ParserConfigurationException {
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
                    if (entry.getPluginChannel() == null)
                        entry.setPluginChannel(getChannel(entry.getVersion()));
                } else if (qName.equals("plugin")) {
                    String id = null;
                    String name = null;
                    String version = null;
                    String channel = null;
                    for (int i = 0; i < attributes.getLength(); i++) {
                        String qname = attributes.getQName(i);
                        if (qname.equals("id")) id = attributes.getValue(i);
                        if (qname.equals("name")) name = attributes.getValue(i);
                        if (qname.equals("version")) version = attributes.getValue(i);
                        if (qname.equals("channel")) channel = attributes.getValue(i);
                    }
                    if (channel == null)
                        channel = getChannel(version);
                    if (id != null) {
                        plugins.add(asList(id, name, version, channel));
                    }
                }
            }
        });
    }

    public static Query getUpdateXmlQueryObj(PersistenceManager pm, UsagePersistenceHelper persistenceHelper) {
        Query query = pm.newQuery();
        query.setClass(persistenceHelper.getDbPluginUpdateXmlClass());
        query.setOrdering("date descending");
        query.setRange(0,1);
        return query;
    }
}
