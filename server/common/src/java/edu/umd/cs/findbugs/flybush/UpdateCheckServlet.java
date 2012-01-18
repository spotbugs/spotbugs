package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Join;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.GoogleAnalytics_v1_URLBuildingStrategy;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

                sendGoogleAnalyicsRequest();
            }
        } finally {
            if (pm.currentTransaction().isActive())
                pm.currentTransaction().rollback();
        }
        //TODO: unit test me!
        resp.setStatus(200);
        resp.setContentType("text/xml");
        try {
            List<DbPluginUpdateXml> results = (List<DbPluginUpdateXml>) getUpdateXmlQueryObj(pm, persistenceHelper).execute();
            if (results.size() > 0)
                resp.getWriter().write(results.get(0).getContents());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception while grabbing update check XML from DB", e);
        }
    }

    private void sendGoogleAnalyicsRequest() {
        /*
                //Google analytics tracking code for Library Finder
                JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(pluginEntry.getAppName(),pluginEntry.getAppVersion(),"UA-95484-8");
                tracker.setUrlBuildingStrategy(new GoogleAnalytics_v1_URLBuildingStrategy(pluginEntry.getAppName(), pluginEntry.getAppVersion(), "UA-95484-8") {
                    @Override
                    public String buildURL(FocusPoint focusPoint) {
                        return super.buildURL(focusPoint) + "&utme=";
                    }
                });


        //                    _gaq.push(['_setCustomVar',
        //                          1,                   // This custom var is set to slot #1.  Required parameter.
        //                          'Section1',           // The top-level name for your online content categories.  Required parameter.
        //                          'Life & Style',  // Sets the value of "Section" to "Life & Style" for this particular aricle.  Required parameter.
        //                          3                    // Sets the scope to page-level.  Optional parameter.
        //                       ]);
        //                    _gaq.push(['_setCustomVar',
        //                          2,                   // This custom var is set to slot #1.  Required parameter.
        //                          'Section2',           // The top-level name for your online content categories.  Required parameter.
        //                          'Life & Style',  // Sets the value of "Section" to "Life & Style" for this particular aricle.  Required parameter.
        //                          3                    // Sets the scope to page-level.  Optional parameter.
        //                       ]);

                // http://www.google-analytics.com/__utm.gif?utmwv=5.2.2&utms=1&utmn=1729318528&utmhn=keithlea.com
                // &utmt=event
                // &utme=5(Shopping*Item%20Removal)8(Section1*Section2)9(Life%20%26%20Style*Life%20%26%20Style)
                // &utmcs=ISO-8859-1&utmsr=1680x1050&utmsc=24-bit&utmul=en-us&utmje=1&utmfl=11.1%20r102
                // &utmdt=Analytics%20Test&utmhid=1462421625&utmr=-&utmp=%2Fin%2Fanalytics-test.html&utmac=UA-95484-8
                // &utmcc=__utma%3D69158283.1665018895.1326831161.1326831161.1326831161.1%3B%2B__utmz%3D69158283.1326831161.1.1.utmcsr%3D(direct)%7Cutmccn%3D(direct)%7Cutmcmd%3D(none)%3B&utmu=4Q~

                Random random = new Random();
                int cookie = random.nextInt();
                int randomValue = random.nextInt(2147483647) -1;
                long now = new Date().getTime();
                LinkedHashMap<String,String> vars = Maps.newLinkedHashMap();
                vars.put("JavaVersion", entry.getJavaVersion());
                vars.put("JavaVersion", entry.getUuid());

                StringBuilder url = new StringBuilder("http://www.google-analytics.com/__utm.gif");
                url.append("?utmwv=1"); //Urchin/Analytics version
                url.append("&utmn=" + random.nextInt());
                url.append("&utmcs=UTF-8"); //document encoding
                url.append("&utmsr=1440x900"); //screen resolution
                url.append("&utmsc=32-bit"); //color depth
                url.append("&utmul=" + pluginEntry.getLanguage() + "-" + pluginEntry.getCountry()); //user language
                url.append("&utmje=1"); //java enabled
                url.append("&utmfl=9.0%20%20r28"); //flash
                url.append("&utmcr=1"); //carriage return
                url.append("&utmhn=findbugs-cloud.appspot.com");//document hostname
        //                url.append("&utmr="+refererURL); //referer URL
                url.append("&utmac=UA-95484-8");//Google Analytics account
                url.append("&utme=5(Usage*Usage)" +
                        "8("
                        + Joiner.on("*").join(vars.keySet()) + ")" +
                        "9("
                        + Joiner.on("*").join(vars.values()) + ")");//Google Analytics account
                url.append("&utmcc=__utma%3D'"+cookie+"."+randomValue+"."+now+"."+now+"."+now+".2%3B%2B__utmb%3D"
                        +cookie+"%3B%2B__utmc%3D"+cookie+"%3B%2B__utmz%3D"+cookie+"."+now
                        +".2.2.utmccn%3D(direct)%7Cutmcsr%3D(direct)%7Cutmcmd%3D(none)%3B%2B__utmv%3D"+cookie);
                return url.toString();
                */
    }

    public static Query getUpdateXmlQueryObj(PersistenceManager pm, PersistenceHelper persistenceHelper) {
        Query query = pm.newQuery();
        query.setClass(persistenceHelper.getDbPluginUpdateXmlClass());
        query.setOrdering("date descending");
        query.setRange(0,1);
        return query;
    }
}
