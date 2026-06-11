package kr.glound.townrise;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TownRiseUpdateManifest {
    private static final Pattern VERSION = Pattern.compile("\\\"version\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern FILES_ARRAY = Pattern.compile("\\\"files\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern OBJECT = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);

    private final String version;
    private final List<ManifestFile> files;

    private TownRiseUpdateManifest(String version, List<ManifestFile> files) {
        this.version = version;
        this.files = List.copyOf(files);
    }

    public String version() {
        return version;
    }

    public List<ManifestFile> files() {
        return files;
    }

    public Optional<ManifestFile> firstFileFor(String path) {
        return files.stream().filter(file -> file.path().equals(path)).findFirst();
    }

    public static TownRiseUpdateManifest parse(String json, URI manifestUri) {
        String version = matchString(VERSION, json).orElse("unknown");
        Matcher filesMatcher = FILES_ARRAY.matcher(json);
        if (!filesMatcher.find()) {
            throw new IllegalArgumentException("manifest files array is missing");
        }

        List<ManifestFile> files = new ArrayList<>();
        Matcher objectMatcher = OBJECT.matcher(filesMatcher.group(1));
        while (objectMatcher.find()) {
            String object = objectMatcher.group(1);
            String path = requiredString(object, "path");
            String name = matchFieldString(object, "name").orElse(path.substring(path.lastIndexOf('/') + 1));
            String sha256 = requiredString(object, "sha256").toLowerCase();
            String url = requiredString(object, "url");
            long size = matchFieldLong(object, "size").orElse(-1L);
            URI resolvedUrl = manifestUri.resolve(url);
            files.add(new ManifestFile(path, name, sha256, size, resolvedUrl));
        }

        if (files.isEmpty()) {
            throw new IllegalArgumentException("manifest files array is empty");
        }
        return new TownRiseUpdateManifest(version, files);
    }

    private static String requiredString(String object, String key) {
        return matchFieldString(object, key)
                .orElseThrow(() -> new IllegalArgumentException("manifest file is missing " + key));
    }

    private static Optional<String> matchFieldString(String object, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
        return matchString(pattern, object).map(TownRiseUpdateManifest::unescapeJsonString);
    }

    private static Optional<Long> matchFieldLong(String object, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(object);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(1)));
    }

    private static Optional<String> matchString(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    private static String unescapeJsonString(String value) {
        return value.replace("\\\\/", "/")
                .replace("\\\\\"", "\"")
                .replace("\\\\\\\\", "\\");
    }

    public record ManifestFile(String path, String name, String sha256, long size, URI url) {
    }
}
