package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatEmojiListener implements Listener {
    private final IconLoader plugin;
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_<3]+):");
    private final Map<UUID, Long> lastEmojiUse = new HashMap<>();

    public ChatEmojiListener(IconLoader plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) return;
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("chat.anti-spam", true)) {
            long cooldown = plugin.getConfig().getInt("chat.cooldown-ms", 1000);
            if (lastEmojiUse.containsKey(player.getUniqueId()) && (System.currentTimeMillis() - lastEmojiUse.get(player.getUniqueId()) < cooldown)) return;
            lastEmojiUse.put(player.getUniqueId(), System.currentTimeMillis());
        }

        Component originalMessage = event.message();
        Map<String, GlyphDefinition> glyphs = plugin.getConfigManager().getGlyphRegistry().getGlyphs();

        TextReplacementConfig config = TextReplacementConfig.builder()
            .match(EMOJI_PATTERN)
            .replacement((matchResult, builder) -> {
                String emojiName = matchResult.group(1).toLowerCase();
                GlyphDefinition glyph = glyphs.get(emojiName);

                if (glyph == null) {
                    for (GlyphDefinition g : glyphs.values()) {
                        if (g.getAliases().stream().anyMatch(a -> a.toLowerCase().equals(emojiName))) { glyph = g; break; }
                    }
                }

                if (glyph != null) {
                    if (!glyph.getPermission().isEmpty() && !player.hasPermission(glyph.getPermission())) return builder;

                    // THE FIX: We build the component but don't override the parent's color/style entirely
                    return Component.text(glyph.getCharacter())
                        .font(CUSTOM_FONT)
                        .decoration(TextDecoration.ITALIC, false);
                }
                return builder;
            }).build();

        // Replace text preserves the original component's structure, color, and hover events
        event.message(originalMessage.replaceText(config));
    }
}