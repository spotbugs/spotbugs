package edu.umd.cs.findbugs;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    @SuppressWarnings({"deprecation"})
    public void testMe() throws Exception {
        final StringBuilder printed = new StringBuilder();
        final StringBuilder errors = new StringBuilder();
        UpdateChecker checker = new UpdateChecker(new UpdateCheckCallback() {
            public void pluginUpdateCheckComplete(List<UpdateChecker.PluginUpdate> updates, boolean force) {
            }

            public String getGlobalOption(String key) {
                return null;
            }

            public Plugin getGlobalOptionSetter(String key) {
                return null;
            }
        }) {
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
        Plugin plugin = new Plugin("my.id", "2.0", KEITHS_BIRTHDAY_2011,  new PluginLoader(), true, false);
        plugin.setShortDescription("My Plugin");
        String xml = "<fb-plugin-updates>" +
                "<plugin id='my.id'>" +
                "<release date='09/01/2011 02:00 PM EST' version='2.1' url='http://example.com/update'>" +
                "<message>UPDATE ME</message>" +
                "</release>" +
                "</plugin>" +
                "</fb-plugin-updates>";
        checker.parseUpdateXml(new URI("http://example.com/update-check"), Arrays.asList(plugin),
                new ByteArrayInputStream(xml.getBytes("UTF-8")));

        assertEquals("PLUGIN UPDATE: My Plugin 2.1 has been released (you have 2.0)\n" +
                "   UPDATE ME\n" +
                "Visit http://example.com/update for downloads and release notes.\n", printed.toString());
    }
}
