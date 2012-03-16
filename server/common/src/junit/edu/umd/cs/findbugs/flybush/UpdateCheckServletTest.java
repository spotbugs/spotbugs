package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

@SuppressWarnings({"UnusedDeclaration"})
public abstract class UpdateCheckServletTest extends AbstractFlybushServletTest {

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new UpdateCheckServlet();
    }

    @SuppressWarnings({"unchecked"})
    public void testTrackSinglePlugin() throws IOException {
        executePost("/update-check", (
                "<findbugs-invocation version='MINE' uuid='UUID' app-name='APPNAME' " +
                "app-version='APPVERSION' entry-point='ENTRYPOINT' os='OS' java-version='JAVAVERSION' " +
                "language='FR' country='CA'>" +
                "  <plugin id='PLUGIN' name='PLUGINNAME' version='PLUGINVERSION'/>" +
                "</findbugs-invocation>").getBytes("UTF-8"));
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbUsageEntryClassname());
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
        assertEquals(1, results.size());
        checkEntry(results.get(0), "PLUGIN", "PLUGINNAME", "PLUGINVERSION");
    }

    @SuppressWarnings({"unchecked"})
    public void testResponse() throws IOException, ParseException {

        release("com.my.plugin", "beta", "2.1.3", "Mar 20, 2011 8:20:00 AM");

        executePost("/update-check", (
                "<findbugs-invocation version='2.0.0-dev-20120113' uuid='UUID' app-name='APPNAME' " +
                        "app-version='APPVERSION' entry-point='ENTRYPOINT' os='OS' java-version='JAVAVERSION' " +
                        "language='FR' country='CA'/>").getBytes("UTF-8"));
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbUsageEntryClassname());
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
//        assertEquals(1, results.size());
//        DbUsageEntry entry = results.get(0);
//        assertEquals("2.0.0-dev-20120113", entry.getVersion());
//        assertEquals("UUID", entry.getUuid());
        checkResponse(200, "text/xml",
                "<?xml version=\"1.0\" ?>\n" +
                        "<fb-plugin-updates>\n" +
                        "  <plugin id=\"com.my.plugin\">\n" +
                        "    <release channel=\"beta\" date=\"03/20/2011 08:20 AM UTC\" version=\"2.1.3\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "</fb-plugin-updates>");
    }

    @SuppressWarnings({"unchecked"})
    public void testResponseMultipleVersions() throws IOException, ParseException {

        release("com.my.plugin", "beta", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin", "beta", "2.1.4", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin", "beta", "2.1.5", "Mar 22, 2011 8:20:00 AM");

        executePost("/update-check", (
                "<findbugs-invocation version='2.0.0-dev-20120113' uuid='UUID' app-name='APPNAME' " +
                        "app-version='APPVERSION' entry-point='ENTRYPOINT' os='OS' java-version='JAVAVERSION' " +
                        "language='FR' country='CA'/>").getBytes("UTF-8"));
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbUsageEntryClassname());
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
        checkResponse(200, "text/xml",
                "<?xml version=\"1.0\" ?>\n" +
                        "<fb-plugin-updates>\n" +
                        "  <plugin id=\"com.my.plugin\">\n" +
                        "    <release channel=\"beta\" date=\"03/22/2011 08:20 AM UTC\" version=\"2.1.5\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "    <release channel=\"beta\" date=\"03/21/2011 08:20 AM UTC\" version=\"2.1.4\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "    <release channel=\"beta\" date=\"03/20/2011 08:20 AM UTC\" version=\"2.1.3\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "</fb-plugin-updates>");
    }

    @SuppressWarnings({"unchecked"})
    public void testResponseMultiplePlugins() throws IOException, ParseException {

        release("com.my.plugin", "beta", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin", "beta", "2.1.4", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin2", "beta", "1.1.3", "Mar 22, 2011 8:20:00 AM");
        release("com.my.plugin2", "beta", "1.1.4", "Mar 23, 2011 8:20:00 AM");

        executePost("/update-check", (
                "<findbugs-invocation version='2.0.0-dev-20120113' uuid='UUID' app-name='APPNAME' " +
                        "app-version='APPVERSION' entry-point='ENTRYPOINT' os='OS' java-version='JAVAVERSION' " +
                        "language='FR' country='CA'/>").getBytes("UTF-8"));
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbUsageEntryClassname());
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
        checkResponse(200, "text/xml",
                "<?xml version=\"1.0\" ?>\n" +
                        "<fb-plugin-updates>\n" +
                        "  <plugin id=\"com.my.plugin\">\n" +
                        "    <release channel=\"beta\" date=\"03/21/2011 08:20 AM UTC\" version=\"2.1.4\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "    <release channel=\"beta\" date=\"03/20/2011 08:20 AM UTC\" version=\"2.1.3\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "  <plugin id=\"com.my.plugin2\">\n" +
                        "    <release channel=\"beta\" date=\"03/23/2011 08:20 AM UTC\" version=\"1.1.4\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "    <release channel=\"beta\" date=\"03/22/2011 08:20 AM UTC\" version=\"1.1.3\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "</fb-plugin-updates>");
    }

    @SuppressWarnings({"unchecked"})
    public void testGuessChannel() throws IOException, ParseException {

        release("com.my.plugin", "stable", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin", "beta", "2.1.4-beta2", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin", "dev", "2.1.4-dev-20120113", "Mar 22, 2011 8:20:00 AM");
        release("com.my.plugin", "rc", "2.1.4-rc1", "Mar 23, 2011 8:20:00 AM");
        release("com.my.plugin", "rc", "2.1.4-rc2", "Mar 24, 2011 8:20:00 AM");

        release("com.my.plugin2", "stable", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin2", "beta", "2.1.4-beta2", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin2", "dev", "2.1.4-dev-20120113", "Mar 22, 2011 8:20:00 AM");
        release("com.my.plugin2", "rc", "2.1.4-rc1", "Mar 23, 2011 8:20:00 AM");
        release("com.my.plugin2", "rc", "2.1.4-rc2", "Mar 24, 2011 8:20:00 AM");

        release("com.my.plugin3", "stable", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin3", "beta", "2.1.4-beta2", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin3", "dev", "2.1.4-dev-20120113", "Mar 22, 2011 8:20:00 AM");
        release("com.my.plugin3", "rc", "2.1.4-rc1", "Mar 23, 2011 8:20:00 AM");
        release("com.my.plugin3", "rc", "2.1.4-rc2", "Mar 24, 2011 8:20:00 AM");
        release("com.my.plugin3", "alpha", "2.1.4-alpha9", "Mar 24, 2011 8:20:00 AM");

        release("com.my.plugin4", "stable", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin4", "beta", "2.1.4-beta2", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin4", "dev", "2.1.4-dev-20120113", "Mar 22, 2011 8:20:00 AM");
        release("com.my.plugin4", "rc", "2.1.4-rc1", "Mar 23, 2011 8:20:00 AM");
        release("com.my.plugin4", "rc", "2.1.4-rc2", "Mar 24, 2011 8:20:00 AM");

        release("com.my.plugin5", "stable", "2.1.3", "Mar 20, 2011 8:20:00 AM");
        release("com.my.plugin5", "beta", "2.1.4-beta2", "Mar 21, 2011 8:20:00 AM");
        release("com.my.plugin5", "dev", "2.1.4-dev-20120113", "Mar 22, 2011 8:20:00 AM");
        release("com.my.plugin5", "rc", "2.1.4-rc1", "Mar 23, 2011 8:20:00 AM");
        release("com.my.plugin5", "rc", "2.1.4-rc2", "Mar 24, 2011 8:20:00 AM");

        executePost("/update-check", (
                "<findbugs-invocation version='XXX' uuid='UUID' app-name='APPNAME' " +
                        "app-version='APPVERSION' entry-point='ENTRYPOINT' os='OS' java-version='JAVAVERSION' " +
                        "language='FR' country='CA'>" +
                        "  <plugin id='com.my.plugin' name='PLUGINNAME1' version='2.0.0-rc1'/>" +
                        "  <plugin id='com.my.plugin2' name='PLUGINNAME1' version='2.0.0-beta2'/>" +
                        "  <plugin id='com.my.plugin3' name='PLUGINNAME1' version='2.0.0-alpha5'/>" +
                        "  <plugin id='com.my.plugin4' name='PLUGINNAME1' version='2.0.0'/>" +
                        "  <plugin id='com.my.plugin5' name='PLUGINNAME1' version='2.0.0-dev-20120320'/>" +
                        "</findbugs-invocation>").getBytes("UTF-8"));
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbUsageEntryClassname());
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
        checkResponse(200, "text/xml",
                "<?xml version=\"1.0\" ?>\n" +
                        "<fb-plugin-updates>\n" +
                        "  <plugin id=\"com.my.plugin\">\n" +
                        "    <release channel=\"rc\" date=\"03/24/2011 08:20 AM UTC\" version=\"2.1.4-rc2\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "    <release channel=\"rc\" date=\"03/23/2011 08:20 AM UTC\" version=\"2.1.4-rc1\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "  <plugin id=\"com.my.plugin2\">\n" +
                        "    <release channel=\"beta\" date=\"03/21/2011 08:20 AM UTC\" version=\"2.1.4-beta2\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "  <plugin id=\"com.my.plugin3\">\n" +
                        "    <release channel=\"alpha\" date=\"03/24/2011 08:20 AM UTC\" version=\"2.1.4-alpha9\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "  <plugin id=\"com.my.plugin4\">\n" +
                        "    <release channel=\"stable\" date=\"03/20/2011 08:20 AM UTC\" version=\"2.1.3\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "  <plugin id=\"com.my.plugin5\">\n" +
                        "    <release channel=\"dev\" date=\"03/22/2011 08:20 AM UTC\" version=\"2.1.4-dev-20120113\" url=\"http://url\">\n" +
                        "      <message><![CDATA[my msg]]></message>\n" +
                        "    </release>\n" +
                        "  </plugin>\n" +
                        "</fb-plugin-updates>");
    }

    private void release(String pluginId, String channel, String version, String date) throws ParseException {
        DateFormat df = DateFormat.getDateTimeInstance();
        DbPluginUpdateXml update = persistenceHelper.createPluginUpdateXml();
        update.setChannel(channel);
        update.setReleaseDate(df.parse(date));
        update.setDate(new Date());
        update.setMessage("my msg");
        update.setPluginId(pluginId);
        update.setUrl("http://url");
        update.setUser("keith@me.com");
        update.setVersion(version);

        PersistenceManager pm = getPersistenceManager();
        pm.currentTransaction().begin();
        pm.makePersistent(update);
        pm.currentTransaction().commit();
    }

    @SuppressWarnings({"unchecked"})
    public void testTrackMultiplePlugins() throws IOException {
        executePost("/update-check", (
                "<findbugs-invocation version='MINE' uuid='UUID' app-name='APPNAME' " +
                        "app-version='APPVERSION' entry-point='ENTRYPOINT' os='OS' java-version='JAVAVERSION' " +
                        "language='FR' country='CA'>" +
                        "  <plugin id='PLUGIN1' name='PLUGINNAME1' version='PLUGINVERSION1'/>" +
                        "  <plugin id='PLUGIN2' name='PLUGINNAME2' version='PLUGINVERSION2'/>" +
                        "</findbugs-invocation>").getBytes("UTF-8"));
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbUsageEntryClassname()
                + " order by plugin ascending");
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
        assertEquals(2, results.size());
        checkEntry(results.get(0), "PLUGIN1", "PLUGINNAME1", "PLUGINVERSION1");
        checkEntry(results.get(1), "PLUGIN2", "PLUGINNAME2", "PLUGINVERSION2");
    }

    private void checkEntry(DbUsageEntry entry, String plugin, String pluginName, String pluginVersion) {
        assertEquals("MINE", entry.getVersion());
        assertEquals("UUID", entry.getUuid());
        assertEquals("APPNAME", entry.getAppName());
        assertEquals("APPVERSION", entry.getAppVersion());
        assertEquals("ENTRYPOINT", entry.getEntryPoint());
        assertEquals("OS", entry.getOs());
        assertEquals("JAVAVERSION", entry.getJavaVersion());
        assertEquals("FR", entry.getLanguage());
        assertEquals("CA", entry.getLocaleCountry());
        assertEquals(plugin, entry.getPlugin());
        assertEquals(pluginName, entry.getPluginName());
        assertEquals(pluginVersion, entry.getPluginVersion());
    }
}
