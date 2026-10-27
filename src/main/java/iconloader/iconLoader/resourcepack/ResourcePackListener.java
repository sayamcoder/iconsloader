package iconloader.iconLoader.resourcepack;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.util.DiscordWebhook;
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
        if (url.isEmpty()) return;

        int delay = plugin.getConfig().getInt("resource-pack.send-delay-ticks", 20);
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.getResourcePackManager().sendPack(player);
        }, delay);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        // Send to Discord Webhook
        if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            DiscordWebhook.send(plugin, "✅ **" + player.getName() + "** accepted the resource pack.");
        } else if (status == PlayerResourcePackStatusEvent.Status.DECLINED ||
                   status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {

            DiscordWebhook.send(plugin, "❌ **" + player.getName() + "** declined/failed to load the resource pack.");

            if (plugin.getConfig().getBoolean("resource-pack.required", false) && plugin.getConfig().getBoolean("resource-pack.kick-on-decline", true)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) player.kick(Component.text("You must accept the resource pack to play."));
                });
            }
        }
    }
}