package iconloader.iconLoader.util;

import iconloader.iconLoader.IconLoader;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordWebhook {
    public static void send(IconLoader plugin, String message) {
        String url = plugin.getConfig().getString("discord-webhook.url", "");
        if (url.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = "{\"content\": \"" + message + "\"}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        });
    }
}