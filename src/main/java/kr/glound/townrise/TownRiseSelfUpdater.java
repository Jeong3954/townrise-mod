package kr.glound.townrise;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TownRiseSelfUpdater {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MANIFEST_PROPERTY = "townrise.updateManifest";
    private static final String MANIFEST_ENV = "TOWNRISE_UPDATE_MANIFEST";
    private static final String TARGET_MOD_PATH = "mods/townrise.jar";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private TownRiseSelfUpdater() {
    }

    public static void startAsync() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(TownRiseSelfUpdater::runSafely, "TownRise Self Updater");
        thread.setDaemon(true);
        thread.start();
    }

    private static void runSafely() {
        try {
            runUpdateCheck();
        } catch (Exception exception) {
            LOGGER.warn("TownRise update check failed", exception);
        }
    }

    private static void runUpdateCheck() throws Exception {
        Optional<URI> manifestUri = manifestUri();
        if (manifestUri.isEmpty()) {
            LOGGER.info("TownRise self-updater is included but disabled. Set -D{}=<manifest-url> or {} to enable it.", MANIFEST_PROPERTY, MANIFEST_ENV);
            return;
        }

        Path currentJar = currentJarPath();
        if (!Files.isRegularFile(currentJar) || !currentJar.getFileName().toString().endsWith(".jar")) {
            LOGGER.info("TownRise self-updater skipped in development classpath: {}", currentJar);
            return;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest manifestRequest = HttpRequest.newBuilder(manifestUri.get())
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        String manifestJson = client.send(manifestRequest, HttpResponse.BodyHandlers.ofString()).body();
        TownRiseUpdateManifest manifest = TownRiseUpdateManifest.parse(manifestJson, manifestUri.get());
        TownRiseUpdateManifest.ManifestFile file = manifest.firstFileFor(TARGET_MOD_PATH)
                .orElse(manifest.files().getFirst());

        String currentHash = sha256(currentJar);
        if (currentHash.equalsIgnoreCase(file.sha256())) {
            LOGGER.info("TownRise is up to date: {}", currentHash);
            return;
        }

        LOGGER.info("TownRise update found: current={} remote={} version={}", currentHash, file.sha256(), manifest.version());
        Path download = currentJar.resolveSibling(currentJar.getFileName() + ".download");
        Path pending = currentJar.resolveSibling(currentJar.getFileName() + ".pending");
        downloadFile(client, file.url(), download);
        if (file.size() >= 0 && Files.size(download) != file.size()) {
            Files.deleteIfExists(download);
            throw new IOException("downloaded size mismatch: expected=" + file.size() + " actual=" + Files.size(download));
        }
        String downloadedHash = sha256(download);
        if (!downloadedHash.equalsIgnoreCase(file.sha256())) {
            Files.deleteIfExists(download);
            throw new IOException("downloaded sha256 mismatch: expected=" + file.sha256() + " actual=" + downloadedHash);
        }
        Files.move(download, pending, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        startApplyScript(pending, currentJar);
        notifyAndExit(manifest.version());
    }

    private static Optional<URI> manifestUri() {
        String value = System.getProperty(MANIFEST_PROPERTY);
        if (value == null || value.isBlank()) {
            value = System.getenv(MANIFEST_ENV);
        }
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(URI.create(value.trim()));
    }

    private static Path currentJarPath() throws Exception {
        URI uri = TownRiseSelfUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return Path.of(uri).toAbsolutePath().normalize();
    }

    private static void downloadFile(HttpClient client, URI uri, Path output) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("download failed with HTTP " + response.statusCode() + " from " + uri);
        }
        try (InputStream input = response.body()) {
            Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
    }

    private static void startApplyScript(Path pending, Path currentJar) throws IOException {
        Path script = currentJar.resolveSibling(isWindows() ? "townrise-apply-update.bat" : "townrise-apply-update.sh");
        if (isWindows()) {
            Files.writeString(script, "@echo off\r\n"
                    + "set SRC=%~1\r\n"
                    + "set DST=%~2\r\n"
                    + ":retry\r\n"
                    + "move /Y \"%SRC%\" \"%DST%\" >nul 2>nul\r\n"
                    + "if errorlevel 1 (\r\n"
                    + "  timeout /t 1 /nobreak >nul\r\n"
                    + "  goto retry\r\n"
                    + ")\r\n");
            new ProcessBuilder("cmd", "/c", "start", "", script.toString(), pending.toString(), currentJar.toString()).start();
        } else {
            Files.writeString(script, "#!/bin/sh\n"
                    + "SRC=\"$1\"\n"
                    + "DST=\"$2\"\n"
                    + "i=0\n"
                    + "while [ $i -lt 90 ]; do\n"
                    + "  if mv \"$SRC\" \"$DST\" 2>/dev/null; then exit 0; fi\n"
                    + "  i=$((i + 1))\n"
                    + "  sleep 1\n"
                    + "done\n"
                    + "exit 1\n");
            script.toFile().setExecutable(true);
            new ProcessBuilder("/bin/sh", script.toString(), pending.toString(), currentJar.toString()).start();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void notifyAndExit(String version) {
        String message = "TownRise 업데이트가 설치되었습니다.\n"
                + "버전: " + version + "\n\n"
                + "Minecraft를 종료합니다. 다시 실행하면 새 버전이 적용됩니다.";
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, "TownRise Update", JOptionPane.INFORMATION_MESSAGE));
        }
        try {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to request Minecraft shutdown after TownRise update", throwable);
        }
        Thread fallbackExit = new Thread(() -> {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }, "TownRise Update Exit");
        fallbackExit.setDaemon(false);
        fallbackExit.start();
    }
}
