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
import java.util.List;
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

    protected void setUp() throws Exception {
        updateCollector = new ArrayList<UpdateChecker.PluginUpdate>();
        errors = new StringBuilder();
        latch = new CountDownLatch(1);
        checker = new UpdateChecker(new TestingUpdateCheckCallback(latch)) {
            @Override
            protected void actuallyCheckforUpdates(URI url, Collection<Plugin> plugins, String entryPoint)
                    throws IOException {
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
        responseXml =
                "<fb-plugin-updates>" +
                "  <plugin id='my.id'>" +
                "    <release date='09/01/2011 02:00 PM EST' version='2.1' url='http://example.com/update'>" +
                "      <message>UPDATE ME</message>" +
                "    </release>" +
                "  </plugin>" +
                "</fb-plugin-updates>";

        // execute
        checkForUpdates(createPlugin("2.0", KEITHS_BIRTHDAY_2011));

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
        responseXml =
                "<fb-plugin-updates>" +
                "  <plugin id='my.id'>" +
                "    <release date='09/01/2011 02:00 PM EST' version='2.0' url='http://example.com/update'>" +
                "      <message>UPDATE ME</message>" +
                "    </release>" +
                "  </plugin>" +
                "</fb-plugin-updates>";

        // execute
        checkForUpdates(createPlugin("2.0", KEITHS_BIRTHDAY_2011));

        // verify
        assertEquals(0, updateCollector.size());
    }

    public void testPluginSameVersionSameDate() throws Exception {
        // setup
        responseXml =
                "<fb-plugin-updates>" +
                "  <plugin id='my.id'>" +
                "    <release date='2011-03-20 02:00:00 EST' version='2.0' url='http://example.com/update'>" +
                "      <message>UPDATE ME</message>" +
                "    </release>" +
                "  </plugin>" +
                "</fb-plugin-updates>";

        // execute
        checkForUpdates(createPlugin("2.0", KEITHS_BIRTHDAY_2011));

        // verify
        assertEquals(0, updateCollector.size());
    }

    public void testPluginDifferentVersionSameDate() throws Exception {
        // setup
        responseXml =
                "<fb-plugin-updates>" +
                "  <plugin id='my.id'>" +
                "    <release date='09/01/2011 02:00 PM EST' version='2.0' url='http://example.com/update'>" +
                "      <message>UPDATE ME</message>" +
                "    </release>" +
                "  </plugin>" +
                "</fb-plugin-updates>";

        // execute
        checkForUpdates(createPlugin("2.0", KEITHS_BIRTHDAY_2011));

        // verify
        assertEquals(0, updateCollector.size());
    }

    // ================ end of tests =============

    private void checkForUpdates(Plugin plugin) throws URISyntaxException, InterruptedException {
        checker.checkForUpdates(Arrays.asList(plugin), true);
        latch.await();
    }

    @SuppressWarnings({"deprecation"})
    private Plugin createPlugin(String version, Date releaseDate) throws URISyntaxException {
        Plugin plugin = new Plugin("my.id", version, releaseDate,  new PluginLoader(), true, false);
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
            return null;
        }

        public Plugin getGlobalOptionSetter(String key) {
            return null;
        }
    }
}
