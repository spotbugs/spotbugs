package edu.umd.cs.findbugs.cwe;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

/**
 * The weakness catalog contains a number of weaknesses
 *
 * @author Jeremias Eppler
 * @see Weakness
 */
public class WeaknessCatalog {
    private static final Logger logger = LoggerFactory.getLogger(WeaknessCatalog.class);

    private String version;

    private String name;
    private LocalDate releaseDate;
    private static final String FILE_NAME = "CWE_4.10.json";
    private final Map<Integer, Weakness> weaknesses = new HashMap<>();

    private static WeaknessCatalog INSTANCE;

    private WeaknessCatalog() {
    }

    /**
     * @return a weakness catalog instance
     */
    public static synchronized WeaknessCatalog getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WeaknessCatalog();
            loadFileAndInitialize();
        }

        return INSTANCE;
    }

    // loads the weaknesses from a JSON file
    private static void loadFileAndInitialize() {
        InputStream inputStream = WeaknessCatalog.class.getClassLoader().getResourceAsStream(FILE_NAME);
        Gson gson = new Gson();
        String characterEncoding = "UTF-8";

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + FILE_NAME);
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(inputStream, characterEncoding))) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            JsonPrimitive nameElement = rootElement.getAsJsonObject().get("name").getAsJsonPrimitive();
            JsonPrimitive versionElement = rootElement.getAsJsonObject().get("version").getAsJsonPrimitive();
            JsonPrimitive dateElement = rootElement.getAsJsonObject().get("date").getAsJsonPrimitive();

            INSTANCE.version = versionElement.getAsString();
            INSTANCE.name = nameElement.getAsString();
            INSTANCE.releaseDate = LocalDate.parse(dateElement.getAsString());

            JsonElement weaknessElements = rootElement.getAsJsonObject().get("weaknesses");

            for (JsonElement weaknessElement : weaknessElements.getAsJsonArray()) {
                Weakness weakness = gson.fromJson(weaknessElement, Weakness.class);

                INSTANCE.weaknesses.put(Integer.valueOf(weakness.getCweId()), weakness);
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Character encoding '{}' is not supported.", characterEncoding);
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            logger.error("Unable to read the weakness catalog JSON.");
        }
    }

    /**
     * @return CWE Version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return name of the weakness catalog (usually: CWE)
     */
    public String getName() {
        return name;
    }

    /**
     * @return the release date of the CWE version
     */
    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    /**
     * Get the a weakness by CWE ID
     *
     * @param cweid
     *            the CWE ID of the weakness
     * @return a copy of the weakness or null
     */
    public Weakness getWeaknessByCweIdOrNull(int cweid) {
        Integer cweId = Integer.valueOf(cweid);

        if (weaknesses.containsKey(cweId)) {
            Weakness weakness = weaknesses.get(cweId);

            // deep copy of the weakness
            return Weakness.of(weakness.getCweId(), weakness.getName(), weakness.getDescription(), weakness.getSeverity());
        }

        return null;
    }
}
