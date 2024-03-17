package edu.umd.cs.findbugs.sarif;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.cwe.Weakness;
import edu.umd.cs.findbugs.cwe.WeaknessCatalog;

class BugCollectionAnalyser {
    @NonNull
    private final List<Rule> rules = new ArrayList<>();
    @NonNull
    private final List<Result> results = new ArrayList<>();
    @NonNull
    private final Map<String, Integer> typeToIndex = new HashMap<>();
    @NonNull
    private final List<List<Placeholder>> indexToPlaceholders = new ArrayList<>();
    @NonNull
    private final SortedSet<Taxon> taxa = new TreeSet<>();

    /**
     * Map baseURI to uriBaseId. e.g. {@code "/user/ubuntu/github/spotbugs/" -> "8736793520"}
     */
    @NonNull
    private final Map<URI, String> baseToId = new HashMap<>();

    BugCollectionAnalyser(@NonNull BugCollection bugCollection) {
        SourceFinder sourceFinder = bugCollection.getProject().getSourceFinder();
        bugCollection.forEach(bug -> {
            String type = bug.getType();
            BugPattern bugPattern = bug.getBugPattern();
            int index = typeToIndex.computeIfAbsent(type, t -> processRule(bugPattern));
            processTaxon(bugPattern.getCWEid());

            processResult(index, bug, sourceFinder);
        });
    }

    JsonArray getRules() {
        JsonArray array = new JsonArray();
        rules.stream().map(Rule::toJsonObject).forEach((jsonObject) -> array.add(jsonObject));
        return array;
    }

    JsonArray getResults() {
        JsonArray array = new JsonArray();
        results.stream().map(Result::toJsonObject).forEach((jsonObject) -> array.add(jsonObject));
        return array;
    }

    JsonObject getCweTaxonomy() {
        JsonObject cweTaxonomy = null;

        if (!taxa.isEmpty()) {
            cweTaxonomy = createCweTaxonomyJson();
        }

        return cweTaxonomy;
    }

    @NonNull
    JsonObject getOriginalUriBaseIds() {
        JsonObject result = new JsonObject();
        baseToId.forEach((uri, uriBaseId) -> {
            JsonObject uriJson = new JsonObject();
            uriJson.addProperty("uri", uri.toString());
            result.add(uriBaseId, uriJson);
        });
        return result;
    }

    private JsonObject createCweTaxonomyJson() {
        JsonObject cweTaxonomy = new JsonObject();

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        String name = weaknessCatalog.getName();
        String version = weaknessCatalog.getVersion();
        LocalDate releaseDate = weaknessCatalog.getReleaseDate();

        UUID cweTaxonomyGuid = GUIDCalculator.fromString(name + version);

        JsonArray taxaJson = new JsonArray();

        taxa.stream().map(Taxon::toJsonObject).forEach((taxonJson) -> taxaJson.add(taxonJson));

        JsonObject shortDescriptionJson = new JsonObject();
        shortDescriptionJson.addProperty("text", "The MITRE Common Weakness Enumeration");

        cweTaxonomy.addProperty("name", name);
        cweTaxonomy.addProperty("version", version);
        cweTaxonomy.addProperty("minimumRequiredLocalizedDataSemanticVersion", version);
        cweTaxonomy.addProperty("releaseDateUtc", releaseDate.toString());
        cweTaxonomy.addProperty("guid", cweTaxonomyGuid.toString());
        cweTaxonomy.addProperty("informationUri", "https://cwe.mitre.org/data/published/cwe_v" + version + ".pdf/");
        cweTaxonomy.addProperty("downloadUri", "https://cwe.mitre.org/data/xml/cwec_v" + version + ".xml.zip");
        cweTaxonomy.addProperty("isComprehensive", true);
        cweTaxonomy.addProperty("organization", "MITRE");
        cweTaxonomy.addProperty("language", "en");
        cweTaxonomy.add("shortDescription", shortDescriptionJson);
        cweTaxonomy.add("taxa", taxaJson);
        return cweTaxonomy;
    }

    private void processResult(int index, BugInstance bug, SourceFinder sourceFinder) {
        List<String> arguments = indexToPlaceholders.get(index).stream()
                .map(placeholder -> placeholder.toArgument(bug.getAnnotations(), bug.getPrimaryClass()))
                .collect(Collectors.toList());
        List<Location> locations = new ArrayList<>();
        Location.fromBugInstance(bug, sourceFinder, baseToId).ifPresent(locations::add);
        int bugRank = BugRanker.findRank(bug);

        Message msg = new Message(arguments);
        msg.text = bug.getBugPattern().getShortDescription();
        Result result = new Result(bug.getType(), index, msg, locations, Level.fromBugRank(bugRank));
        results.add(result);
    }

    private int processRule(BugPattern bugPattern) {
        assert indexToPlaceholders.size() == rules.size();
        int ruleIndex = rules.size();

        List<Placeholder> placeholders = new ArrayList<>();
        MessageFormat formatter = new MessageFormat(bugPattern.getLongDescription());
        String formattedMessage = formatter.format((Integer index, String key) -> {
            int indexOfPlaceholder = placeholders.size();
            placeholders.add(new Placeholder(index, key));
            return String.format("{%d}", indexOfPlaceholder);
        });
        Rule rule = Rule.fromBugPattern(bugPattern, formattedMessage);
        rules.add(rule);
        indexToPlaceholders.add(placeholders);

        return ruleIndex;
    }

    private void processTaxon(int cweid) {
        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        UUID cweTaxonomyGuid = GUIDCalculator.fromString(weaknessCatalog.getName() + weaknessCatalog.getVersion());

        if (weakness != null) {
            UUID cweGuid = GUIDCalculator.fromNamespaceAndString(cweTaxonomyGuid, String.valueOf(cweid));
            Level severityLevel = Level.fromWeaknessSeverity(weakness.getSeverity());
            Taxon taxon = Taxon.from(String.valueOf(weakness.getCweId()), cweGuid, weakness.getName(),
                    weakness.getDescription(), severityLevel);

            taxa.add(taxon);
        }
    }

    Map<URI, String> getBaseToId() {
        return baseToId;
    }
}
