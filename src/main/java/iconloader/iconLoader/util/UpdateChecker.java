package iconloader.iconLoader.util;

import iconloader.iconLoader.IconLoader;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class UpdateChecker implements Listener {
    private static final String UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=";

    private final IconLoader plugin;
    private volatile String latestVersion;

    public UpdateChecker(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        if (!isEnabled()) return;
        String resourceId = configuredResourceId();
        if (resourceId == null) {
            plugin.getLogger().info("update-checker.resource-id is not set, so no version check was attempted.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            latestVersion = fetchLatestVersion(resourceId);
            if (latestVersion != null && isUpdateAvailable()) {
                plugin.getLogger().warning("IconsLoader " + latestVersion + " is available, you run "
                        + currentVersion() + ".");
            }
        });
    }

    /** Runs a check immediately and reports the outcome to whoever asked for it. */
    public void checkNow(CommandSender replyTo) {
        if (!isEnabled()) {
            replyTo.sendMessage(plugin.getLangManager().getMessage("update-disabled"));
            return;
        }
        String resourceId = configuredResourceId();
        if (resourceId == null) {
            replyTo.sendMessage(plugin.getLangManager().getMessage("update-not-configured"));
            return;
        }
        replyTo.sendMessage(plugin.getLangManager().getMessage("update-checking"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Read on the main thread above and passed in, so the worker never touches the config.
            String fetched = fetchLatestVersion(resourceId);
            latestVersion = fetched;
            Component outcome;
            if (fetched == null) {
                // Reporting "you are up to date" here would claim knowledge the request never gave us.
                outcome = plugin.getLangManager().getMessage("update-check-failed");
            } else if (compareVersions(fetched, currentVersion()) > 0) {
                outcome = plugin.getLangManager().getMessage("update-available",
                        "<latest>", fetched, "<current>", currentVersion());
            } else {
                outcome = plugin.getLangManager().getMessage("update-latest", "<current>", currentVersion());
            }
            Bukkit.getScheduler().runTask(plugin, () -> replyTo.sendMessage(outcome));
        });
    }

    private String fetchLatestVersion(String resourceId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPDATE_URL + resourceId))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body().trim() : null;
        } catch (Exception e) {
            plugin.getLogger().fine("Update check failed: " + e.getMessage());
            return null;
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("update-checker.enabled", true);
    }

    public boolean isUpdateAvailable() {
        return latestVersion != null && compareVersions(latestVersion, currentVersion()) > 0;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("update-checker.notify-on-join", true)) return;
        if (!isUpdateAvailable()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("iconsloader.admin")) return;

        player.sendMessage(plugin.getLangManager().getMessage("update-available",
                "<latest>", latestVersion, "<current>", currentVersion()));
    }

    private String configuredResourceId() {
        if (!isEnabled()) return null;
        String resourceId = plugin.getConfig().getString("update-checker.resource-id", "").trim();
        // The placeholder shipped in old builds would have requested a URL that always 404s.
        if (resourceId.isEmpty() || resourceId.equalsIgnoreCase("YOUR_RESOURCE_ID")) return null;
        return resourceId.matches("\\d+") ? resourceId : null;
    }

    private String currentVersion() {
        return plugin.getVersion();
    }

    private static int compareVersions(String latest, String current) {
        String[] left = normalize(latest).split("\\.");
        String[] right = normalize(current).split("\\.");
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int a = i < left.length ? numeric(left[i]) : 0;
            int b = i < right.length ? numeric(right[i]) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static String normalize(String version) {
        return version.toLowerCase(Locale.ROOT).replaceAll("[^0-9.]+", "");
    }

    private static int numeric(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
