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

import java.util.Map;
import java.util.regex.Pattern;

public class ChatEmojiListener implements Listener {
    private final IconLoader plugin;
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    public ChatEmojiListener(IconLoader plugin) {
        this.plugin = plugin;
    }

    // We use HIGHEST priority so we process the chat AFTER other plugins have added their prefixes/colors
    // ignoreCancelled = true means we won't process messages if another plugin cancelled them (e.g., muted players)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) return;

        Player player = event.getPlayer();
        Component originalMessage = event.message();
        Map<String, GlyphDefinition> glyphs = plugin.getConfigManager().getGlyphRegistry().getGlyphs();

        TextReplacementConfig config = TextReplacementConfig.builder()
            .match(EMOJI_PATTERN)
            .replacement((matchResult, builder) -> {
                String emojiName = matchResult.group(1).toLowerCase();
                GlyphDefinition glyph = glyphs.get(emojiName);

                if (glyph != null) {
                    if (!glyph.getPermission().isEmpty() && !player.hasPermission(glyph.getPermission())) {
                        return builder;
                    }
                    return Component.text(glyph.getCharacter())
                        .font(CUSTOM_FONT)
                        .decoration(TextDecoration.ITALIC, false);
                }
                return builder;
            })
            .build();

        event.message(originalMessage.replaceText(config));
    }
}