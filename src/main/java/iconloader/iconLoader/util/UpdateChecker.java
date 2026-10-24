package iconloader.iconLoader.util;

import iconloader.iconLoader.IconLoader;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UpdateChecker {
    private final IconLoader plugin;
    private boolean enabled = true;
    private boolean notifyOnJoin = true;
    private String latestVersion = null;

    public UpdateChecker(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // REPLACE 'YOUR_RESOURCE_ID' WITH YOUR ACTUAL SPIGOT ID ONCE PUBLISHED
                String url = "https://api.spigotmc.org/legacy/update.php?resource=YOUR_RESOURCE_ID";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    latestVersion = response.body().trim();
                }
            } catch (Exception ignored) {}
        });
    }

    public boolean isUpdateAvailable() {
        return latestVersion != null && !latestVersion.equals(plugin.getPluginMeta().getVersion());
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
    public void setNotifyOnJoin(boolean notify) { this.notifyOnJoin = notify; }
    public boolean isNotifyOnJoin() { return notifyOnJoin; }
}