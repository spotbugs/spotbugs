package edu.umd.cs.findbugs.updates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.Version;

public class UpdateCheckerTest extends TestCase {
    private static final Date KEITHS_BIRTHDAY_2011;

    static {
        try {
            KEITHS_BIRTHDAY_2011 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH).parse("2011-03-20 02:00:00 EST");
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
    private String uploadedXml;

    @Override
    protected void setUp() throws Exception {
        updateCollector = new ArrayList<UpdateChecker.PluginUpdate>();
        errors = new StringBuilder();
        latch = new CountDownLatch(1);
        checked = new HashMap<String, Collection<Plugin>>();
        globalOptions = new HashMap<String, String>();
        uploadedXml = null;
        checker = new UpdateChecker(new TestingUpdateCheckCallback(latch)) {
            @Override
            protected void actuallyCheckforUpdates(URI url, Collection<Plugin> plugins, String entryPoint)
                    throws IOException {
                String urlStr = url.toString();
                assertFalse(checked.containsKey(urlStr));
                checked.put(urlStr, plugins);
                ByteArrayInputStream stream = new ByteArrayInputStream(responseXml.getBytes("UTF-8"));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                writeXml(out, plugins, "x.y.z", true);
                uploadedXml = new String(out.toByteArray(), "UTF-8");
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
        org.junit.Assume.assumeTrue(!checker.updateChecksGloballyDisabled());
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
        setResponseXml("my.id", "03/20/2011 03:00 AM EDT", "2.0");

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

    public void testSubmittedXml() throws Exception {
        // setup
        setResponseXml("my.id", "09/01/2011 02:00 PM EST", "2.1");
        Version.registerApplication("MyApp", "2.x");

        // execute
        checkForUpdates(createPlugin("my.id", KEITHS_BIRTHDAY_2011, "2.0"));

        // verify
        String pattern = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "\n" +
                "<findbugs-invocation version='*' app-name='MyApp' app-version='2.x' " +
                "entry-point='x.y.z' os='*' java-version='*' language='*' country='*' " +
                "uuid='*'>\n" +
                "  <plugin id='my.id' name='My Plugin' version='2.0' release-date='1300604400000'/>\n" +
                "</findbugs-invocation>\n";
        String patternRE = convertGlobToRE(pattern);
        assertTrue(uploadedXml + " did not match " + patternRE, uploadedXml.matches(patternRE));
    }

    // ================ end of tests =============

    private void checkForUpdates(Plugin plugin) throws  InterruptedException {

        checker.checkForUpdates(Arrays.asList(plugin), true);
        latch.await();
    }

    private String convertGlobToRE(String pattern) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String blah : pattern.split("\\*")) {
            if (first) {
                first = false;
            } else {
                builder.append(".*");
            }
            builder.append(Pattern.quote(blah.replaceAll("'", "\"")));
        }
        return builder.toString();
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
    private Plugin createPlugin(String pluginId, Date releaseDate, String version) throws URISyntaxException, PluginException {
        PluginLoader fakeLoader;
        try {
            fakeLoader = new PluginLoader(true, new URL("http://" + pluginId + ".findbugs.cs.umd.edu"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Plugin plugin = new Plugin(pluginId, version, releaseDate, fakeLoader,  true, false);
        plugin.setShortDescription("My Plugin");
        plugin.setUpdateUrl("http://example.com/update");
        return plugin;
    }

    private class TestingUpdateCheckCallback implements UpdateCheckCallback {
        private final CountDownLatch latch;

        public TestingUpdateCheckCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void pluginUpdateCheckComplete(List<UpdateChecker.PluginUpdate> updates, boolean force) {
            updateCollector.addAll(updates);
            latch.countDown();
        }

        @Override
        public String getGlobalOption(String key) {
            return globalOptions.get(key);
        }

        @Override
        public Plugin getGlobalOptionSetter(String key) {
            return null;
        }
    }
}
