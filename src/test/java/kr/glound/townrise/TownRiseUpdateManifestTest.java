package kr.glound.townrise;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownRiseUpdateManifestTest {
    @Test
    void parsesRelativeUpdateFileUrl() {
        String json = """
                {
                  "schemaVersion": 1,
                  "version": "0.1.1-dev",
                  "files": [
                    {
                      "path": "mods/townrise.jar",
                      "name": "townrise-0.1.1-dev.jar",
                      "sha256": "abcdef",
                      "size": 123,
                      "url": "files/townrise-0.1.1-dev.jar"
                    }
                  ]
                }
                """;

        TownRiseUpdateManifest manifest = TownRiseUpdateManifest.parse(json, URI.create("https://updates.example/townrise/manifest.json"));

        assertEquals("0.1.1-dev", manifest.version());
        assertEquals(1, manifest.files().size());
        TownRiseUpdateManifest.ManifestFile file = manifest.firstFileFor("mods/townrise.jar").orElseThrow();
        assertEquals("townrise-0.1.1-dev.jar", file.name());
        assertEquals("abcdef", file.sha256());
        assertEquals(123, file.size());
        assertEquals(URI.create("https://updates.example/townrise/files/townrise-0.1.1-dev.jar"), file.url());
    }

    @Test
    void requiresAtLeastOneFile() {
        String json = """
                {
                  "version": "0.1.1-dev",
                  "files": []
                }
                """;

        try {
            TownRiseUpdateManifest.parse(json, URI.create("https://updates.example/townrise/manifest.json"));
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("empty"));
            return;
        }
        throw new AssertionError("expected manifest parser to reject empty files array");
    }
}
