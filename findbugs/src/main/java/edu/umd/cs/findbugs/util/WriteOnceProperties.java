package edu.umd.cs.findbugs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class WriteOnceProperties extends Properties {

    private static final long serialVersionUID = 1L;
    private final Map<String, PropertyReadAt> propertReadAt = new HashMap<String, PropertyReadAt>();

    static class PropertyReadAt extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private WriteOnceProperties(Properties initialValue) {
        super.putAll(initialValue);
    }

    @Override
    public final synchronized int hashCode() {
        return super.hashCode();
    }

    /* overridden to avoid EQ_DOESNT_OVERRIDE_EQUALS */
    @Override
    public final synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public synchronized String getProperty(String key) {
        String result = super.getProperty(key);
        if (result != null && result.length() > 0 && !propertReadAt.containsKey(key)) {
            propertReadAt.put(key, new PropertyReadAt());
        }
        return result;
    }

    @Override
    public synchronized String getProperty(String key, String defaultValue) {
        String result = super.getProperty(key, defaultValue);
        if (result != null && result.length() > 0 && !propertReadAt.containsKey(key)) {
            propertReadAt.put(key, new PropertyReadAt());
        }
        return result;
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        if (propertReadAt.containsKey(key) && !value.equals(super.getProperty(key))) {
            IllegalStateException e = new IllegalStateException("Changing property '" + key + "' to '" + value
                    + "' after it has already been read as '" + super.getProperty(key) + "'");
            e.initCause(propertReadAt.get(key));
            throw e;
        }
        return super.setProperty(key, value);
    }

    public static void makeSystemPropertiesWriteOnce() {
        Properties properties = System.getProperties();
        if (properties instanceof WriteOnceProperties) {
            return;
        }
        System.setProperties(new WriteOnceProperties(properties));
    }

    public static void main(String args[]) {
        dumpProperties();
        System.out.println("-----");
        makeSystemPropertiesWriteOnce();
        dumpProperties();
        System.setProperty("x", "1");
        System.setProperty("y", "1");
        System.getProperty("y");
        System.setProperty("x", "2");
        System.setProperty("y", "2");

    }

    private static void dumpProperties() {

        Properties properties = System.getProperties();
        System.out.println("Total properties: " + properties.size());
        for (Object k : properties.keySet()) {
            System.out.println(k + " : " + System.getProperty((String) k));
        }
    }
}
