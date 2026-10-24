package iconloader.iconLoader.resourcepack;

import iconloader.iconLoader.IconLoader;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.UUID;

public class ResourcePackManager {
    private final IconLoader plugin;

    public ResourcePackManager(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void sendPack(Player player) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;

        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url.isEmpty()) return;

        String sha1String = plugin.getConfig().getString("resource-pack.sha1", "");
        if (sha1String.isEmpty()) {
            plugin.getLogger().warning("SHA-1 hash is missing in config.yml! The pack may not send correctly.");
            return;
        }

        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);

        Component prompt = MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("resource-pack.prompt-message", "<gold>Please accept the resource pack to see custom emojis!")
        );

        // Build the ResourcePackInfo using the modern Adventure API
        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(url))
                .hash(sha1String)
                .build();

        // Build the request.
        // If "force-prompt-on-join" is true, we set required to true to force the screen,
        // otherwise we respect the config.
        boolean forcePrompt = plugin.getConfig().getBoolean("resource-pack.force-prompt-on-join", false);
        boolean shouldBeRequired = required || forcePrompt;

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .prompt(prompt)
                .required(shouldBeRequired)
                .build();

        // Send using the Adventure API
        player.sendResourcePacks(request);
    }

    public void sendPackToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPack(p);
        }
    }
}