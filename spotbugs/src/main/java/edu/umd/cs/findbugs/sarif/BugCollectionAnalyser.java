package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

class BugCollectionAnalyser {
    @NonNull
    private final List<Rule> rules = new ArrayList<>();
    @NonNull
    private final List<Result> results = new ArrayList<>();
    @NonNull
    private final Map<String, Integer> typeToIndex = new HashMap<>();
    @NonNull
    private final List<List<Placeholder>> indexToPlaceholders = new ArrayList<>();

    /**
     * Map baseURI to uriBaseId. e.g. {@code "/user/ubuntu/github/spotbugs/" -> "8736793520"}
     */
    @NonNull
    private final Map<URI, String> baseToId = new HashMap<>();

    BugCollectionAnalyser(@NonNull BugCollection bugCollection) {
        SourceFinder sourceFinder = bugCollection.getProject().getSourceFinder();
        bugCollection.forEach(bug -> {
            String type = bug.getType();
            int index = typeToIndex.computeIfAbsent(type, t -> processRule(bug.getBugPattern()));

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

    Map<URI, String> getBaseToId() {
        return baseToId;
    }
}
