package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.util.EmojiTokenizer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatEmojiListener implements Listener {
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");
    private static final String EMOJI_PERMISSION = "iconsloader.use";

    private final IconLoader plugin;
    private final Map<UUID, Long> lastEmojiUse = new HashMap<>();

    public ChatEmojiListener(IconLoader plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("chat.enabled", true)) return;

        Player player = event.getPlayer();

        if (config.getBoolean("chat.anti-spam", true)) {
            long cooldown = config.getLong("chat.cooldown-ms", 1000);
            Long last = lastEmojiUse.get(player.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < cooldown) return;
            lastEmojiUse.put(player.getUniqueId(), System.currentTimeMillis());
        }

        boolean caseInsensitive = config.getBoolean("chat.case-insensitive", true);
        boolean requirePermission = config.getBoolean("chat.require-permission", true);
        boolean preserveUnknown = config.getBoolean("chat.preserve-unknown-tokens", true);
        Pattern pattern = EmojiTokenizer.pattern(config);

        Component originalMessage = event.message();
        Component formatted = originalMessage.replaceText(TextReplacementConfig.builder()
                .match(pattern)
                .replacement((matchResult, builder) -> {
                    GlyphDefinition glyph = plugin.getConfigManager().getGlyphRegistry()
                            .findByToken(matchResult.group(1), caseInsensitive);

                    // Unknown token: keep the literal ':name:' or drop it, depending on the config.
                    if (glyph == null) return preserveUnknown ? builder : Component.empty();
                    if (!glyph.isChatEnabled()) return builder;
                    if (requirePermission && !player.hasPermission(EMOJI_PERMISSION)) return builder;
                    if (!glyph.getPermission().isEmpty() && !player.hasPermission(glyph.getPermission())) {
                        return builder;
                    }

                    // Only the glyph character itself switches font; the surrounding component keeps
                    // the colour, decorations and hover events the chat format already applied.
                    return Component.text(glyph.getCharacter())
                            .font(CUSTOM_FONT)
                            .decoration(TextDecoration.ITALIC, false);
                })
                .build());

        event.message(formatted);
    }
}
