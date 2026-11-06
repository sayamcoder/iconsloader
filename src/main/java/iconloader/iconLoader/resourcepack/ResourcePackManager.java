package iconloader.iconLoader.resourcepack;

import iconloader.iconLoader.IconLoader;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.URISyntaxException;

public class ResourcePackManager {
    private final IconLoader plugin;

    public ResourcePackManager(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void sendPack(Player player) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;

        String url = plugin.getConfig().getString("resource-pack.url", "").trim();
        if (url.isEmpty()) return;

        URI packUri;
        try {
            packUri = new URI(url);
            if (packUri.getHost() == null) throw new URISyntaxException(url, "missing host");
        } catch (URISyntaxException e) {
            plugin.getLogger().warning("resource-pack.url '" + url + "' is not a valid URL: " + e.getReason());
            return;
        }

        String sha1String = plugin.getConfig().getString("resource-pack.sha1", "").trim();
        if (!sha1String.matches("(?i)[0-9a-f]{40}")) {
            plugin.getLogger().warning("resource-pack.sha1 is empty or not a 40 character hash, so the pack was not sent."
                    + " Run /cl pack generate to fill it in.");
            return;
        }

        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        boolean forcePrompt = plugin.getConfig().getBoolean("resource-pack.force-prompt-on-join", false);

        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(packUri)
                .hash(sha1String)
                .build();

        var request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .required(required || forcePrompt);

        if (plugin.getConfig().getBoolean("resource-pack.prompt-on-join", true)) {
            request.prompt(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("resource-pack.prompt-message",
                            "<gold>Please accept the resource pack to see the custom emojis!")));
        }

        player.sendResourcePacks(request.build());
    }

    public void sendPackToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPack(p);
        }
    }

    /** Says out loud what would otherwise look like a silently broken install. */
    public void warnIfUrlMissing() {
        if (!plugin.getConfig().getBoolean("resource-pack.warn-if-url-missing", true)) return;

        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) {
            // Hosting the pack while refusing to send it is a dead end that produces no symptom anywhere else.
            if (plugin.getConfig().getBoolean("http-server.enabled", false)) {
                plugin.getLogger().warning("http-server is hosting the pack, but resource-pack.enabled is false, so no pack is ever sent.");
                plugin.getLogger().warning("Set resource-pack.enabled: true to deliver it.");
            }
            return;
        }
        if (!plugin.getConfig().getString("resource-pack.url", "").trim().isEmpty()) return;

        plugin.getLogger().warning("resource-pack.enabled is true but resource-pack.url is empty, so players get no pack.");
        plugin.getLogger().warning("Either set the URL yourself, or enable http-server so IconsLoader hosts and fills it in.");
    }
}
