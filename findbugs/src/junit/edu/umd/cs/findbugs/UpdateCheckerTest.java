package edu.umd.cs.findbugs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import junit.framework.TestCase;

public class UpdateCheckerTest extends TestCase {
    private static final Date KEITHS_BIRTHDAY_2011;

    static {
        try {
            KEITHS_BIRTHDAY_2011 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").parse("2011-03-20 02:00:00 EST");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<UpdateChecker.PluginUpdate> updateCollector;
    private StringBuilder errors;
    private String responseXml;
    private CountDownLatch latch;
    private UpdateChecker checker;
    private Map<String, Collection<Plugin>> checked;
    private Map<String, String> globalOptions;

    protected void setUp() throws Exception {
        updateCollector = new ArrayList<UpdateChecker.PluginUpdate>();
        errors = new StringBuilder();
        latch = new CountDownLatch(1);
        checked = new HashMap<String, Collection<Plugin>>();
        globalOptions = new HashMap<String, String>();
        checker = new UpdateChecker(new TestingUpdateCheckCallback(latch)) {
            @Override
            protected void actuallyCheckforUpdates(URI url, Collection<Plugin> plugins, String entryPoint)
                    throws IOException {
                String urlStr = url.toString();
                assertFalse(checked.containsKey(urlStr));
                checked.put(urlStr, plugins);
                ByteArrayInputStream stream = new ByteArrayInputStream(responseXml.getBytes("UTF-8"));
                parseUpdateXml(url, plugins, stream);
            }

            @Override
            protected void logError(Exception e, String msg) {
                errors.append(msg).append("\n");
                System.err.println(msg);
                e.printStackTrace();
            }

            @Override
            protected void logError(Level level, String msg) {
                errors.append(msg).append("\n");
                System.err.println(msg);
            }
        };
    }

    public void testSimplePluginUpdate() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.1");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(1, updateCollector.size());
        UpdateChecker.PluginUpdate update = updateCollector.get(0);
        assertEquals("UPDATE ME", update.getMessage());
        assertEquals("http://example.com/update", update.getUrl());
        assertEquals("2.1", update.getVersion());
        assertEquals("my.id", update.getPlugin().getPluginId());
    }

    public void testPluginSameVersionDifferentDate() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.0");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(0, updateCollector.size());
    }

    public void testPluginSameVersionSameDate() throws Exception {
        // setup
        setResponseXml("my.id", "2011-03-20 02:00:00 EST", "2.0");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(0, updateCollector.size());
    }

    public void testPluginDifferentVersionSameDate() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.0");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(0, updateCollector.size());
    }

    public void testPluginNotPresent() throws Exception {
        // setup
        setResponseXml("SOME.OTHER.PLUGIN", "09/01/2011 02:00 PM EST", "2.0");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(0, updateCollector.size());
    }

    public void testRedirectUpdateChecks() throws Exception {
        // setup
        setResponseXml("SOME.OTHER.PLUGIN", "09/01/2011 02:00 PM EST", "2.0");
        globalOptions.put("redirectUpdateChecks", "http://redirect.com");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(1, checked.size());
        assertTrue(checked.containsKey("http://redirect.com"));
        assertEquals(0, updateCollector.size());
    }

    public void testDisableUpdateChecks() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.1");
        globalOptions.put("noUpdateChecks", "true");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(0, checked.size());
        assertEquals(0, updateCollector.size());
    }

    public void testDisableUpdateChecksFalse() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.1");
        globalOptions.put("noUpdateChecks", "false");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        assertEquals(1, checked.size());
        assertEquals(1, updateCollector.size());
    }

    public void testDisableUpdateChecksInvalid() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.1");
        globalOptions.put("noUpdateChecks", "BLAH");

        // execute
        try {
            checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));
            fail();
        } catch (Throwable e) {
        }

        // verify
        assertEquals(0, checked.size());
        assertEquals(0, updateCollector.size());
    }

    // ================ end of tests =============

    private void checkForUpdates(Plugin plugin) throws URISyntaxException, InterruptedException {
        checker.checkForUpdates(Arrays.asList(plugin), true);
        latch.await();
    }

    private void setResponseXml(String pluginid, String releaseDate, String v) {
        responseXml =
                "<fb-plugin-updates>" +
                        "  <plugin id='" + pluginid + "'>" +
                        "    <release date='" + releaseDate + "' version='" + v + "' url='http://example.com/update'>" +
                        "      <message>UPDATE ME</message>" +
                        "    </release>" +
                        "  </plugin>" +
                        "</fb-plugin-updates>";
    }

    @SuppressWarnings({"deprecation"})
    private Plugin createPlugin(String pluginId, Date releaseDate, String version) throws URISyntaxException {
        Plugin plugin = new Plugin(pluginId, version, releaseDate, new PluginLoader(), true, false);
        plugin.setShortDescription("My Plugin");
        plugin.setUpdateUrl("http://example.com/update");
        return plugin;
    }

    private class TestingUpdateCheckCallback implements UpdateCheckCallback {
        private final CountDownLatch latch;

        public TestingUpdateCheckCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        public void pluginUpdateCheckComplete(List<UpdateChecker.PluginUpdate> updates, boolean force) {
            updateCollector.addAll(updates);
            latch.countDown();
        }

        public String getGlobalOption(String key) {
            return globalOptions.get(key);
        }

        public Plugin getGlobalOptionSetter(String key) {
            return null;
        }
    }
}
