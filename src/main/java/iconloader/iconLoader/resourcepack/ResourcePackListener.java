package iconloader.iconLoader.resourcepack;

import iconloader.iconLoader.IconLoader;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class ResourcePackListener implements Listener {
    private final IconLoader plugin;

    public ResourcePackListener(IconLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;

        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isEmpty()) {
            if (plugin.getConfig().getBoolean("resource-pack.warn-if-url-missing", true)) {
                plugin.getLogger().warning("Resource pack URL is missing in config.yml!");
            }
            return;
        }

        int delay = plugin.getConfig().getInt("resource-pack.send-delay-ticks", 20);
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getResourcePackManager().sendPack(player);
            }
        }, delay);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("resource-pack.required", false)) return;
        if (!plugin.getConfig().getBoolean("resource-pack.kick-on-decline", true)) return;

        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        if (status == PlayerResourcePackStatusEvent.Status.DECLINED ||
            status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kick(Component.text("You must accept the resource pack to play on this server."));
                }
            });
        }
    }
}