public class UnnecessaryEnvUsage {
    public void replaceableEnvUsage() {
        System.getenv("JAVA_HOME");
        System.getenv("JAVA_VERSION");
        System.getenv("TEMP");
        System.getenv("TMP");
        System.getenv("PROCESSOR_ARCHITECTURE");
        System.getenv("OS");
        System.getenv("USER");
        System.getenv("USERNAME");
        System.getenv("HOME");
        System.getenv("HOMEPATH");
        System.getenv("CD");
        System.getenv("PWD");
    }

    public void necessaryEnvUsage() {
        System.getenv("CUSTOM_ENV_FIELD");
    }

    public void compliantPropertyUsage() {
        System.getProperty("user.name");
    }
}
