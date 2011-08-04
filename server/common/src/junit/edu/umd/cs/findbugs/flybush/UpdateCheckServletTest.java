package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.List;

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
                + " order by plugin");
        List<DbUsageEntry> results = (List<DbUsageEntry>) query.execute();
        assertEquals(2, results.size());
        checkEntry(results.get(0), "PLUGIN1", "PLUGINNAME1", "PLUGINVERSION1");
        checkEntry(results.get(1), "PLUGIN2", "PLUGINNAME2", "PLUGINVERSION2");
    }
}
