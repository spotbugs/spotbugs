import java.util.Map;

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

    public void replaceableEnvUsageFromMap() {
        System.getenv().get("JAVA_HOME");
    }

    public void replaceableEnvUsageFromMapWithVar() {
        Map<String,String> envVars = System.getenv();
        envVars.get("JAVA_VERSION");
        envVars.get("TEMP");
    }

    public void replaceableEnvUsageWithVar() {
        String envName = "TMP";
        System.getenv(envName);
    }

    private static final String ARCH = "PROCESSOR_ARCHITECTURE";
    public void replaceableEnvUsageWithField() {
        System.getenv(ARCH);
    }

    public void replaceableEnvUsageWithVarFromMap() {
        String envName = "OS";
        System.getenv().get(envName);
    }

    private static final String USER = "USER";
    public void replaceableEnvUsageWithFieldFromMap() {
        System.getenv().get(USER);
    }

    public void replaceableEnvUsageWithVarFromMapWithVar() {
        Map<String,String> envVars = System.getenv();
        String envUser = "USER";
        String envUserName = "USERNAME";
        envVars.get(envUser);
        envVars.get(envUserName);
    }

    private static final String HOME = "HOME";
    private static final String HOMEPATH = "HOMEPATH";
    public void replaceableEnvUsageWithFieldFromMapWithVar() {
        Map<String,String> envVars = System.getenv();
        envVars.get(HOME);
        envVars.get(HOMEPATH);
    }

    public void necessaryEnvUsage() {
        System.getenv("CUSTOM_ENV_FIELD");
    }

    public void necessaryEnvUsageFromMap() {
        System.getenv().get("CUSTOM_ENV_FIELD");
    }

    public void necessaryEnvUsageFromMapWithVar() {
        Map<String,String> envVars = System.getenv();
        envVars.get("PATH");
    }

    public void necessaryEnvUsageWithVar() {
        String custom_field = "CUSTOM_ENV_FIELD";
        System.getenv("CUSTOM_ENV_FIELD");
    }

    public void necessaryEnvUsageWithVarFromMap() {
        String custom_field = "CUSTOM_ENV_FIELD";
        System.getenv().get(custom_field);
    }

    public void necessaryEnvUsageWithVarFromMapWithVar() {
        Map<String,String> envVars = System.getenv();
        String custom_field = "CUSTOM_ENV_FIELD";
        envVars.get(custom_field);
    }

    private static String CUSTOM_ENV_FIELD = "CUSTOM_ENV_FIELD";
    public void necessaryEnvUsageWithField() {
        System.getenv(CUSTOM_ENV_FIELD);
    }

    public void necessaryEnvUsageWithFieldFromMap() {
        System.getenv().get(CUSTOM_ENV_FIELD);
    }

    public void necessaryEnvUsageWithFieldFromMapWithVar() {
        Map<String,String> envVars = System.getenv();
        envVars.get(CUSTOM_ENV_FIELD);
    }

    public void compliantPropertyUsage() {
        System.getProperty("user.name");
    }
}
