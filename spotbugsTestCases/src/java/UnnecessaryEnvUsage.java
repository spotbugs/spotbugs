public class UnnecessaryEnvUsage {
    public void replaceableEnvUsage() {
        System.getenv("USER");
        System.getenv("USERNAME");
    }

    public void necessaryEnvUsage() {
        System.getenv("CUSTOM_ENV_FIELD");
    }

    public void compliantPropertyUsage() {
        System.getProperty("user.name");
    }
}
